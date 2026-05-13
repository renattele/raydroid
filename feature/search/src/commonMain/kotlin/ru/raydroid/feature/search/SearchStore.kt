package ru.raydroid.feature.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.CloseCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.application.usecase.EnterItemUseCase
import ru.raydroid.plugin.host.api.application.usecase.ExecuteCommandCallbackUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetCommandFullscreenUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetSearchFieldRequestsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.application.usecase.UpdateCommandQueryUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.event.NotificationEvent.AlertResult
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.api.ui.PluginFormSubmitCallback
import ru.raydroid.plugin.host.api.ui.PluginFormValues
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.pluginFocusModel

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchStore(
    private val applicationScope: CoroutineScope,
    private val syncCacheUseCase: SyncCacheUseCase,
    private val loadRuntimesUseCase: LoadRuntimesUseCase,
    private val searchUseCase: SearchUseCase,
    private val getPluginsUseCase: GetPluginsUseCase,
    private val openCommandUseCase: OpenCommandUseCase,
    private val enterItemUseCase: EnterItemUseCase,
    private val closeCommandUseCase: CloseCommandUseCase,
    private val executeCommandCallbackUseCase: ExecuteCommandCallbackUseCase,
    private val getCommandFullscreenUseCase: GetCommandFullscreenUseCase,
    private val getEventsUseCase: GetEventsUseCase,
    private val emitEventUseCase: EmitEventUseCase,
    private val getSearchFieldRequestsUseCase: GetSearchFieldRequestsUseCase,
    private val updateCommandQueryUseCase: UpdateCommandQueryUseCase
) {
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private var sessionScope: CoroutineScope? = null
    private var fullscreenJob: Job? = null
    private val toastDismissJobs = mutableMapOf<String, Job>()
    private var pendingOpenCommandName: String? = null

    fun start() {
        if (sessionScope != null) return
        val scope = CoroutineScope(applicationScope.coroutineContext + SupervisorJob())
        sessionScope = scope

        scope.launch {
            syncCacheUseCase()
        }
        scope.launch {
            loadRuntimesUseCase()
        }
        scope.launch {
            getPluginsUseCase().collectLatest { plugins ->
                val pluginMap = plugins.associateBy { it.pluginId }
                val currentState = _state.value
                if (currentState.fullscreen != null) {
                    _state.update { state ->
                        state.copy(plugins = pluginMap)
                    }
                    focusFullscreenItem(currentState.fullscreen.focusedItemId)
                } else {
                    val focusedActions = currentState.searchResults.actionsForFocused(
                        currentState.focusedItemIndex,
                        pluginMap
                    )
                    _state.update { state ->
                        state.copy(
                            plugins = pluginMap,
                            focusedActions = focusedActions,
                            showActions = state.showActions && focusedActions.isNotEmpty()
                        )
                    }
                }
            }
        }
        scope.launch {
            getEventsUseCase().collectLatest { event ->
                when (val data = event.data) {
                    is NotificationEvent.Alert -> {
                        _state.update { state ->
                            state.copy(alerts = state.alerts + data)
                        }
                    }

                    is NotificationEvent.ShowToast -> {
                        showToast(data)
                    }

                    is NotificationEvent.HideToast -> {
                        hideToast(data.toastId)
                    }
                }
            }
        }
        scope.launch {
            _state
                .map { state -> state.searchFieldState.text }
                .distinctUntilChanged()
                .onEach {
                    _state.update { state ->
                        state.copy(
                            isSearching = state.searchResults == null,
                            focusedItemIndex = state.searchResults
                                ?.takeIf { results -> results.isNotEmpty() }
                                ?.let { 0 },
                            focusedActions = emptyList(),
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false
                        )
                    }
                }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query ->
                    var focusInitialized = false
                    searchUseCase(query).collectLatest { searchResults ->
                        val uiResults = searchResults.results.map { result -> result.toUiModel() }
                        val currentState = _state.value
                        val focusedIndex = if (!focusInitialized) {
                            uiResults.takeIf { results -> results.isNotEmpty() }?.let { 0 }
                        } else {
                            uiResults.takeIf { results -> results.isNotEmpty() }?.let { results ->
                                currentState.focusedItemIndex?.coerceIn(0, results.lastIndex)
                            }
                        }
                        focusInitialized = true
                        val focusedActions = uiResults.actionsForFocused(
                            focusedIndex,
                            currentState.plugins
                        )
                        _state.update { state ->
                            state.copy(
                                searchResults = uiResults,
                                focusedItemIndex = focusedIndex,
                                isSearching = false,
                                focusedActions = focusedActions,
                                showActions = state.showActions && focusedActions.isNotEmpty(),
                                contextActions = emptyList(),
                                showContextActions = false
                            )
                        }

                        val pendingCommand = pendingOpenCommandName
                        val commandResult = searchResults.results.firstOrNull { result ->
                            result is SearchResultSet.CommandSearchResult &&
                                result.resultId.commandName == pendingCommand
                        } as? SearchResultSet.CommandSearchResult
                        if (pendingCommand != null && commandResult != null) {
                            pendingOpenCommandName = null
                            collectFullscreen(commandResult.toCommandUiModel())
                            openCommandUseCase(commandResult.resultId)
                        }
                    }
                }
        }
        scope.launch {
            _state
                .map { state ->
                    state.fullscreen?.let { fullscreen ->
                        FullscreenQuery(
                            resultId = fullscreen.resultId,
                            query = fullscreen.searchFieldState.text
                        )
                    }
                }
                .distinctUntilChanged()
                .flatMapLatest { fullscreenQuery ->
                    if (fullscreenQuery == null) {
                        flowOf(null)
                    } else {
                        flowOf(fullscreenQuery)
                    }
                }
                .onEach { fullscreenQuery ->
                    if (fullscreenQuery?.query?.isNotEmpty() == true) {
                        _state.update { state ->
                            val fullscreen = state.fullscreen ?: return@update state
                            if (fullscreen.resultId != fullscreenQuery.resultId) {
                                state
                            } else {
                                state.copy(
                                    fullscreen = fullscreen.copy(exitBackspaceCount = 0)
                                )
                            }
                        }
                    }
                }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { fullscreenQuery ->
                    if (fullscreenQuery != null) {
                        updateCommandQueryUseCase(
                            resultId = fullscreenQuery.resultId,
                            query = fullscreenQuery.query
                        )
                        focusFullscreenItem(_state.value.fullscreen?.focusedItemId)
                    }
                }
        }
        scope.launch {
            getSearchFieldRequestsUseCase().collectLatest { request ->
                val fullscreen = _state.value.fullscreen ?: return@collectLatest
                if (fullscreen.resultId.pluginId != request.pluginId) {
                    return@collectLatest
                }
                _state.update { state ->
                    val currentFullscreen = state.fullscreen ?: return@update state
                    if (currentFullscreen.resultId.pluginId != request.pluginId) {
                        state
                    } else {
                        state.copy(
                            fullscreen = currentFullscreen.copy(
                                searchFieldState = SearchQueryState(
                                    text = request.state.text,
                                    selection = request.state.selection
                                ),
                                exitBackspaceCount = 0
                            )
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        fullscreenJob?.cancel()
        fullscreenJob = null
        toastDismissJobs.values.forEach { job -> job.cancel() }
        toastDismissJobs.clear()
        sessionScope?.cancel()
        sessionScope = null
    }

    fun currentState(): SearchUiState = _state.value

    fun watch(observer: (SearchUiState) -> Unit): SearchStoreWatchHandle {
        val job = applicationScope.launch {
            state.collectLatest { current ->
                observer(current)
            }
        }
        return SearchStoreWatchHandle { job.cancel() }
    }

    fun resourceResolver(language: String): PluginResourceResolver {
        return PluginResourceResolver(
            plugins = _state.value.plugins,
            language = language
        )
    }

    fun openSearch(query: String = "") {
        pendingOpenCommandName = null
        _state.update { state ->
            state.copy(
                searchFieldState = SearchQueryState(
                    text = query,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun openCommand(commandName: String) {
        pendingOpenCommandName = commandName
        _state.update { state ->
            state.copy(
                searchFieldState = SearchQueryState(
                    text = commandName,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun updateRootQuery(query: String) {
        if (pendingOpenCommandName != null && pendingOpenCommandName != query) {
            pendingOpenCommandName = null
        }
        _state.update { state ->
            if (state.searchFieldState.text == query) {
                state
            } else {
                state.copy(
                    searchFieldState = SearchQueryState(
                        text = query,
                        selection = SearchFieldSelection.CursorAtEnd
                    )
                )
            }
        }
    }

    fun updateFullscreenQuery(query: String) {
        _state.update { state ->
            val fullscreen = state.fullscreen ?: return@update state
            if (fullscreen.searchFieldState.text == query) {
                state
            } else {
                state.copy(
                    fullscreen = fullscreen.copy(
                        searchFieldState = SearchQueryState(
                            text = query,
                            selection = SearchFieldSelection.CursorAtEnd
                        )
                    )
                )
            }
        }
    }

    fun enter(resultId: SearchResultId? = null) {
        launch { handleEnter(resultId) }
    }

    fun enterAction(action: ActionUiModel) {
        launch {
            executeCommandCallbackUseCase(
                resultId = action.resultId,
                callback = action.action.callback,
                updateUsage = action.updateUsage
            )
        }
    }

    fun enterCallback(
        resultId: SearchResultId,
        callback: PluginCommandCallback,
        updateUsage: Boolean = false
    ) {
        launch {
            executeCommandCallbackUseCase(
                resultId = resultId,
                callback = callback,
                updateUsage = updateUsage
            )
        }
    }

    fun submitForm(
        callback: PluginFormSubmitCallback,
        values: PluginFormValues
    ) {
        launch {
            callback(values)
        }
    }

    fun moveFocusNext() {
        launch {
            val state = _state.value
            if (state.fullscreen != null) {
                moveFullscreenFocus(1)
                return@launch
            }
            val focusedIndex = state.searchResults?.let { searchResults ->
                state.focusedItemIndex?.let { itemIndex ->
                    (itemIndex + 1).coerceIn(0, searchResults.lastIndex)
                }
            }
            val focusedActions = state.searchResults.actionsForFocused(
                focusedIndex,
                state.plugins
            )
            _state.update { current ->
                current.copy(
                    focusedItemIndex = focusedIndex,
                    focusedActions = focusedActions,
                    showActions = false,
                    contextActions = emptyList(),
                    showContextActions = false
                )
            }
        }
    }

    fun moveFocusPrevious() {
        launch {
            val state = _state.value
            if (state.fullscreen != null) {
                moveFullscreenFocus(-1)
                return@launch
            }
            val focusedIndex = state.searchResults?.let { searchResults ->
                state.focusedItemIndex?.let { itemIndex ->
                    (itemIndex - 1).coerceIn(0, searchResults.lastIndex)
                }
            }
            val focusedActions = state.searchResults.actionsForFocused(
                focusedIndex,
                state.plugins
            )
            _state.update { current ->
                current.copy(
                    focusedItemIndex = focusedIndex,
                    focusedActions = focusedActions,
                    showActions = false,
                    contextActions = emptyList(),
                    showContextActions = false
                )
            }
        }
    }

    fun toggleActions() {
        _state.update { state ->
            state.copy(
                showActions = !state.showActions,
                showContextActions = false
            )
        }
    }

    fun hideActions() {
        _state.update { state ->
            state.copy(
                showActions = false,
                showContextActions = false
            )
        }
    }

    fun backspaceOnEmpty() {
        launch {
            val state = _state.value
            val fullscreen = state.fullscreen ?: return@launch
            val exitBackspaceCount = (fullscreen.exitBackspaceCount + 1).coerceAtMost(2)
            _state.update { currentState ->
                val currentFullscreen = currentState.fullscreen ?: return@update currentState
                currentState.copy(
                    fullscreen = currentFullscreen.copy(
                        exitBackspaceCount = exitBackspaceCount
                    )
                )
            }
            if (exitBackspaceCount >= 2) {
                collapseAndCloseFullscreen(fullscreen.resultId)
            }
        }
    }

    fun dismissAlert(alert: NotificationEvent.Alert) {
        launch {
            _state.update { state ->
                state.copy(alerts = state.alerts - alert)
            }
            emitEventUseCase(
                alert.pluginId,
                AlertResult(NotificationEvent.Selection.Dismiss)
            )
        }
    }

    fun confirmAlert(alert: NotificationEvent.Alert) {
        launch {
            _state.update { state ->
                state.copy(alerts = state.alerts - alert)
            }
            emitEventUseCase(
                alert.pluginId,
                AlertResult(NotificationEvent.Selection.Confirm)
            )
        }
    }

    fun hideToast(toastId: String) {
        dismissToast(toastId, cancelJob = true)
    }

    fun showContextActions(
        resultId: SearchResultId,
        actions: List<PluginCommandListAction>
    ) {
        _state.update { state ->
            state.copy(
                contextActions = actions.map { action ->
                    ActionUiModel(
                        resultId = resultId,
                        action = action,
                        updateUsage = false
                    )
                },
                showContextActions = actions.isNotEmpty(),
                showActions = false
            )
        }
    }

    fun focusPluginItem(itemId: CommandItemId) {
        launch { focusFullscreenItem(itemId) }
    }

    fun enterPluginItem(itemId: CommandItemId) {
        launch { enterFullscreenItem(itemId) }
    }

    fun closeFullscreen() {
        launch {
            val fullscreen = _state.value.fullscreen ?: return@launch
            collapseAndCloseFullscreen(fullscreen.resultId)
        }
    }

    private suspend fun handleEnter(resultId: SearchResultId?) {
        val state = _state.value
        val fullscreen = state.fullscreen
        if (fullscreen != null) {
            enterFocusedFullscreenItem(fullscreen)
            return
        }
        val openResultId = resultId ?: state.focusedResultId()
        if (openResultId == null) {
            return
        }
        val openResult = state.searchResults
            ?.firstOrNull { result -> result.resultId == openResultId }
        when (openResult) {
            is SearchResultUiModel.Command -> {
                collectFullscreen(openResult)
                openCommandUseCase(openResultId)
            }

            is SearchResultUiModel.Live -> {
                val primaryCallback = openResult.presentation.primaryCallback
                if (primaryCallback != null) {
                    executeCommandCallbackUseCase(
                        resultId = openResultId,
                        callback = primaryCallback,
                        updateUsage = false
                    )
                }
            }

            is SearchResultUiModel.Cached -> {
                enterItemUseCase(openResultId)
            }

            null -> Unit
        }
    }

    private fun launch(block: suspend () -> Unit) {
        val scope = sessionScope ?: applicationScope
        scope.launch { block() }
    }

    private suspend fun collapseAndCloseFullscreen(resultId: SearchResultId) {
        _state.update { state ->
            val fullscreen = state.fullscreen ?: return@update state
            if (fullscreen.resultId != resultId) {
                state
            } else {
                state.copy(
                    fullscreen = fullscreen.copy(exitBackspaceCount = 2)
                )
            }
        }
        delay(FULLSCREEN_COLLAPSE_DELAY_MS)
        val state = _state.value
        if (state.fullscreen?.resultId == resultId) {
            closeFullscreen(state)
        }
    }

    private suspend fun closeFullscreen(state: SearchUiState) {
        val fullscreen = state.fullscreen ?: return
        fullscreenJob?.cancel()
        fullscreenJob = null
        closeCommandUseCase(fullscreen.resultId)
        _state.update { currentState ->
            currentState.copy(fullscreen = null)
        }
    }

    private fun showToast(toast: NotificationEvent.ShowToast) {
        _state.update { state ->
            state.copy(toasts = state.toasts + toast)
        }
        val autoDismissMillis = toast.toast.autoDismissMillis ?: return
        toastDismissJobs.remove(toast.toastId)?.cancel()
        val scope = sessionScope ?: applicationScope
        toastDismissJobs[toast.toastId] = scope.launch {
            delay(autoDismissMillis)
            dismissToast(toast.toastId, cancelJob = false)
        }
    }

    private fun dismissToast(toastId: String, cancelJob: Boolean) {
        val job = toastDismissJobs.remove(toastId)
        if (cancelJob) {
            job?.cancel()
        }
        _state.update { state ->
            state.copy(
                toasts = state.toasts.filterNot { toast -> toast.toastId == toastId }
            )
        }
    }

    private suspend fun moveFullscreenFocus(delta: Int) {
        val fullscreen = _state.value.fullscreen ?: return
        val query = fullscreen.searchFieldState.text
        val model = fullscreen.content.pluginFocusModel(fullscreen.focusedItemId, query)
        val currentIndex = model.items.indexOfFirst { item -> item.id == model.focusedItemId }
        if (currentIndex < 0) return
        val nextIndex = (currentIndex + delta).coerceIn(0, model.items.lastIndex)
        focusFullscreenItem(model.items[nextIndex].id)
    }

    private suspend fun focusFullscreenItem(
        itemId: CommandItemId?,
        notifyPlugin: Boolean = true
    ) {
        val state = _state.value
        val fullscreen = state.fullscreen ?: return
        val query = fullscreen.searchFieldState.text
        val model = fullscreen.content.pluginFocusModel(itemId, query)
        val focusedItem = model.focusedItem
        val currentFocusedActions = state.focusedActions
            .takeIf {
                fullscreen.focusedItemId == model.focusedItemId &&
                    it.all { action -> action.resultId == fullscreen.resultId }
            }
            ?.map { action -> action.action }
            .orEmpty()
        val focusedActions = focusedItem?.actions
            ?.takeIf { actions -> actions.isNotEmpty() }
            ?: currentFocusedActions.takeIf { actions -> actions.isNotEmpty() }
            ?: fullscreen.resultId.let { resultId ->
                focusedItem?.let { item ->
                    state.plugins[resultId.pluginId]?.actions(resultId.commandName, item.id)
                }
            }.orEmpty()
        _state.update { currentState ->
            val currentFullscreen = currentState.fullscreen ?: return@update currentState
            if (currentFullscreen.resultId != fullscreen.resultId) {
                currentState
            } else {
                currentState.copy(
                    fullscreen = currentFullscreen.copy(
                        focusedItemId = model.focusedItemId
                    ),
                    focusedActions = focusedActions.map { action ->
                        ActionUiModel(
                            resultId = fullscreen.resultId,
                            action = action,
                            updateUsage = false
                        )
                    },
                    showActions = currentState.showActions && focusedActions.isNotEmpty(),
                    contextActions = emptyList(),
                    showContextActions = false
                )
            }
        }
        if (notifyPlugin && fullscreen.focusedItemId != model.focusedItemId) {
            state.plugins[fullscreen.resultId.pluginId]?.update(
                fullscreen.resultId.commandName,
                CommandActionBridge.Regular(CommandAction.Focus(model.focusedItemId))
            )
        }
    }

    private suspend fun enterFocusedFullscreenItem(fullscreen: FullscreenUiState) {
        val itemId = fullscreen.focusedItemId ?: return
        enterFullscreenItem(itemId)
    }

    private suspend fun enterFullscreenItem(itemId: CommandItemId) {
        val state = _state.value
        val fullscreen = state.fullscreen ?: return
        focusFullscreenItem(itemId)
        val focusedAction = _state.value.focusedActions
            .firstOrNull { action -> action.action.primary }
            ?: _state.value.focusedActions.firstOrNull()
        if (focusedAction != null) {
            executeCommandCallbackUseCase(
                resultId = focusedAction.resultId,
                callback = focusedAction.action.callback,
                updateUsage = false
            )
        } else {
            state.plugins[fullscreen.resultId.pluginId]?.update(
                fullscreen.resultId.commandName,
                CommandActionBridge.Regular(CommandAction.Enter(itemId))
            )
        }
    }

    private fun collectFullscreen(result: SearchResultUiModel.Command) {
        fullscreenJob?.cancel()
        val runtime = _state.value.plugins[result.resultId.pluginId]
        val command = runtime
            ?.manifest
            ?.commands
            ?.firstOrNull { command -> command.service == result.resultId.commandName }
        _state.update { state ->
            state.copy(
                fullscreen = FullscreenUiState(
                    resultId = result.resultId,
                    title = result.listEntry.title,
                    placeholder = command?.placeholder?.toPluginUiText(result.resultId.pluginId),
                    searchFieldState = state.searchFieldState,
                    exitBackspaceCount = 0,
                    content = emptyList(),
                    focusedItemId = null
                ),
                focusedActions = emptyList(),
                showActions = false,
                contextActions = emptyList(),
                showContextActions = false
            )
        }
        launch {
            val focusedActions = result.actions(_state.value.plugins).map { action ->
                ActionUiModel(
                    resultId = result.resultId,
                    action = action,
                    updateUsage = false
                )
            }
            _state.update { state ->
                val fullscreen = state.fullscreen
                if (fullscreen?.resultId == result.resultId) {
                    val query = fullscreen.searchFieldState.text
                    if (fullscreen.content.pluginFocusModel(fullscreen.focusedItemId, query).focusedItemId != null) {
                        state
                    } else {
                        state.copy(
                            focusedActions = focusedActions,
                            showActions = state.showActions && focusedActions.isNotEmpty()
                        )
                    }
                } else {
                    state
                }
            }
        }
        val scope = sessionScope ?: applicationScope
        fullscreenJob = scope.launch {
            getCommandFullscreenUseCase(result.resultId).collectLatest { content ->
                val newContent = content?.content.orEmpty()
                _state.update { state ->
                    val fullscreen = state.fullscreen
                    if (fullscreen?.resultId != result.resultId) {
                        state
                    } else {
                        state.copy(
                            fullscreen = fullscreen.copy(
                                content = newContent
                            )
                        )
                    }
                }
                focusFullscreenItem(_state.value.fullscreen?.focusedItemId)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 24L
        const val FULLSCREEN_COLLAPSE_DELAY_MS = 160L
    }
}

data class SearchUiState(
    val searchFieldState: SearchQueryState = SearchQueryState(),
    val searchResults: List<SearchResultUiModel>? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedActions: List<ActionUiModel> = emptyList(),
    val contextActions: List<ActionUiModel> = emptyList(),
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false,
    val showActions: Boolean = false,
    val showContextActions: Boolean = false,
    val fullscreen: FullscreenUiState? = null,
    val alerts: List<NotificationEvent.Alert> = emptyList(),
    val toasts: List<NotificationEvent.ShowToast> = emptyList()
)

data class SearchQueryState(
    val text: String = "",
    val selection: SearchFieldSelection = SearchFieldSelection.CursorAtEnd
)

data class FullscreenUiState(
    val resultId: SearchResultId,
    val title: PluginUiText?,
    val placeholder: PluginUiText?,
    val searchFieldState: SearchQueryState,
    val exitBackspaceCount: Int,
    val content: List<PluginRayNodeData>,
    val focusedItemId: CommandItemId?
)

data class ActionUiModel(
    val resultId: SearchResultId,
    val action: PluginCommandListAction,
    val updateUsage: Boolean = true
)

sealed interface SearchResultUiModel {
    val resultId: SearchResultId
    val listEntry: ru.raydroid.plugin.host.api.ui.PluginCommandListItem

    data class Cached(
        override val resultId: SearchResultId,
        override val listEntry: ru.raydroid.plugin.host.api.ui.PluginCommandListItem,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    ) : SearchResultUiModel

    data class Command(
        override val resultId: SearchResultId,
        override val listEntry: ru.raydroid.plugin.host.api.ui.PluginCommandListItem
    ) : SearchResultUiModel

    data class Live(
        override val resultId: SearchResultId,
        override val listEntry: ru.raydroid.plugin.host.api.ui.PluginCommandListItem,
        val presentation: PluginCommandPresentation
    ) : SearchResultUiModel
}

class SearchStoreWatchHandle(
    private val closeBlock: () -> Unit
) {
    fun close() {
        closeBlock()
    }
}

private data class FullscreenQuery(
    val resultId: SearchResultId,
    val query: String
)

private fun SearchUiState.focusedResultId(): SearchResultId? =
    focusedItemIndex?.let { itemIndex ->
        searchResults?.getOrNull(itemIndex)?.resultId
    }

private fun SearchResultSet.SearchResult.toUiModel(): SearchResultUiModel = when (this) {
    is SearchResultSet.CachedSearchResult -> SearchResultUiModel.Cached(
        resultId = resultId,
        listEntry = listEntry,
        titleMatches = titleMatches,
        descriptionMatches = descriptionMatches
    )

    is SearchResultSet.CommandSearchResult -> SearchResultUiModel.Command(
        resultId = resultId,
        listEntry = listEntry
    )

    is SearchResultSet.LiveSearchResult -> SearchResultUiModel.Live(
        resultId = resultId,
        listEntry = listEntry,
        presentation = presentation
    )
}

private fun SearchResultSet.CommandSearchResult.toCommandUiModel(): SearchResultUiModel.Command {
    return SearchResultUiModel.Command(
        resultId = resultId,
        listEntry = listEntry
    )
}

private suspend fun List<SearchResultUiModel>?.actionsForFocused(
    index: Int?,
    plugins: Map<PluginId, PluginRuntime>
): List<ActionUiModel> {
    val result = index?.let { itemIndex -> this?.getOrNull(itemIndex) } ?: return emptyList()
    val actions = result.actions(plugins)
    return actions.map { action ->
        ActionUiModel(
            resultId = result.resultId,
            action = action
        )
    }
}

private suspend fun SearchResultUiModel.actions(
    plugins: Map<PluginId, PluginRuntime>
): List<PluginCommandListAction> {
    return when (this) {
        is SearchResultUiModel.Live -> presentation.actions
        is SearchResultUiModel.Cached,
        is SearchResultUiModel.Command -> plugins[resultId.pluginId]?.actions(
            commandName = resultId.commandName,
            itemId = resultId.itemId
        ).orEmpty()
    }
}

private fun UiText.toPluginUiText(pluginId: PluginId): PluginUiText = when (type) {
    UiText.Type.Plain -> PluginUiText.Plain(text)
    UiText.Type.Resource -> PluginUiText.Resource(pluginId = pluginId, key = text)
}
