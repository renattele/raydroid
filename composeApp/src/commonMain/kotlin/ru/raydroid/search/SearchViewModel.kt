package ru.raydroid.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import ru.raydroid.plugin.host.api.event.NotificationEvent.*
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.api.ui.toPluginUiText
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState as ApiSearchFieldState
import ru.raydroid.plugin.host.impl.presentation.SearchFieldState as PresentationSearchFieldState

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
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
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchScreenState(
            eventSink = ::onEvent
        )
    )
    val state = _state.asStateFlow()
    private var fullscreenJob: Job? = null
    private val toastDismissJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncCacheUseCase()
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadRuntimesUseCase()
        }
        viewModelScope.launch {
            getPluginsUseCase().collectLatest { plugins ->
                val pluginMap = plugins.associateBy { it.pluginId }
                val currentState = _state.value
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
        viewModelScope.launch {
            getEventsUseCase().collectLatest { event ->
                when (val data = event.data) {
                    is NotificationEvent.Alert -> {
                        _state.update { state ->
                            state.copy(
                                alerts = state.alerts + data
                            )
                        }
                    }

                    is NotificationEvent.ShowToast -> {
                        showToast(data)
                    }

                    is NotificationEvent.HideToast -> {
                        hideToast(data)
                    }
                }
            }
        }
        viewModelScope.launch {
            snapshotFlow { _state.value.searchFieldState.fieldState.text }
                .map { text -> text.toString() }
                .distinctUntilChanged()
                .onEach {
                    _state.update { state ->
                        state.copy(
                            isSearching = state.searchResults == null,
                            focusedItemIndex = state.searchResults
                                ?.results
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
                        val currentState = _state.value
                        val focusedIndex = if (!focusInitialized) {
                            searchResults.results.takeIf { it.isNotEmpty() }?.let { 0 }
                        } else {
                            searchResults.results.takeIf { it.isNotEmpty() }?.let { results ->
                                currentState.focusedItemIndex?.coerceIn(0, results.lastIndex)
                            }
                        }
                        focusInitialized = true
                        val focusedActions = searchResults.actionsForFocused(
                            focusedIndex,
                            currentState.plugins
                        )
                        _state.update { state ->
                            state.copy(
                                searchResults = searchResults,
                                focusedItemIndex = focusedIndex,
                                isSearching = false,
                                focusedActions = focusedActions,
                                showActions = state.showActions && focusedActions.isNotEmpty(),
                                contextActions = emptyList(),
                                showContextActions = false
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            _state
                .map { state ->
                    state.fullscreen?.let { fullscreen ->
                        FullscreenSearchField(
                            resultId = fullscreen.resultId,
                            fieldState = fullscreen.searchFieldState.fieldState
                        )
                    }
                }
                .distinctUntilChanged()
                .flatMapLatest { fullscreenSearchField ->
                    if (fullscreenSearchField == null) {
                        flowOf(null)
                    } else {
                        snapshotFlow {
                            FullscreenQuery(
                                resultId = fullscreenSearchField.resultId,
                                query = fullscreenSearchField.fieldState.text.toString()
                            )
                        }
                    }
                }
                .distinctUntilChanged()
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
                    focusFullscreenItem(_state.value.fullscreen?.focusedItemId)
                }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { fullscreenQuery ->
                    if (fullscreenQuery != null) {
                        updateCommandQueryUseCase(
                            resultId = fullscreenQuery.resultId,
                            query = fullscreenQuery.query
                        )
                    }
                }
        }
        viewModelScope.launch {
            getSearchFieldRequestsUseCase().collectLatest { request ->
                val fullscreen = _state.value.fullscreen ?: return@collectLatest
                if (fullscreen.resultId.pluginId != request.pluginId) {
                    return@collectLatest
                }
                fullscreen.searchFieldState.fieldState.apply(request.state)
                _state.update { state ->
                    val currentFullscreen = state.fullscreen ?: return@update state
                    if (currentFullscreen.resultId.pluginId != request.pluginId) {
                        state
                    } else {
                        state.copy(
                            fullscreen = currentFullscreen.copy(exitBackspaceCount = 0)
                        )
                    }
                }
            }
        }
    }

    private fun onEvent(event: SearchScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is SearchScreenEvent.Enter -> {
                    val state = _state.value
                    val fullscreen = state.fullscreen
                    if (fullscreen != null) {
                        enterFocusedFullscreenItem(fullscreen)
                        return@launch
                    }
                    val openResultId =
                        event.resultId ?: state.focusedResultId()
                    if (openResultId == null) {
                        return@launch
                    }
                    val openResult = state.searchResults
                        ?.results
                        ?.firstOrNull { result -> result.resultId == openResultId }
                    when (openResult) {
                        is SearchResultSet.CommandSearchResult -> {
                            collectFullscreen(openResult)
                            openCommandUseCase(openResultId)
                        }
                        is SearchResultSet.LiveSearchResult -> {
                            val primaryCallback = openResult.presentation.primaryCallback
                            if (primaryCallback != null) {
                                executeCommandCallbackUseCase(
                                    resultId = openResultId,
                                    callback = primaryCallback,
                                    updateUsage = false
                                )
                            }
                        }
                        else -> {
                            enterItemUseCase(openResultId)
                        }
                    }
                }

                SearchScreenEvent.MoveFocusNext -> {
                    val state = _state.value
                    if (state.fullscreen != null) {
                        moveFullscreenFocus(1)
                        return@launch
                    }
                    val focusedIndex = state.searchResults?.let { searchResults ->
                        state.focusedItemIndex?.let { itemIndex ->
                            (itemIndex + 1).coerceIn(0, searchResults.results.lastIndex)
                        }
                    }
                    val focusedActions = state.searchResults.actionsForFocused(
                        focusedIndex,
                        state.plugins
                    )
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = focusedIndex,
                            focusedActions = focusedActions,
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false
                        )
                    }
                }

                SearchScreenEvent.MoveFocusPrevious -> {
                    val state = _state.value
                    if (state.fullscreen != null) {
                        moveFullscreenFocus(-1)
                        return@launch
                    }
                    val focusedIndex = state.searchResults?.let { searchResults ->
                        state.focusedItemIndex?.let { itemIndex ->
                            (itemIndex - 1).coerceIn(0, searchResults.results.lastIndex)
                        }
                    }
                    val focusedActions = state.searchResults.actionsForFocused(
                        focusedIndex,
                        state.plugins
                    )
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = focusedIndex,
                            focusedActions = focusedActions,
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false
                        )
                    }
                }

                SearchScreenEvent.ToggleActions -> {
                    _state.update { state ->
                        state.copy(
                            showActions = !state.showActions,
                            showContextActions = false
                        )
                    }
                }

                SearchScreenEvent.HideActions -> {
                    _state.update { state ->
                        state.copy(
                            showActions = false,
                            showContextActions = false
                        )
                    }
                }

                is SearchScreenEvent.QueryChanged -> {

                }

                SearchScreenEvent.BackspaceOnEmpty -> {
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

                is SearchScreenEvent.DismissAlert -> {
                    _state.update { state ->
                        state.copy(
                            alerts = state.alerts - event.alert
                        )
                    }
                    emitEventUseCase.invoke(
                        event.alert.pluginId,
                        AlertResult(NotificationEvent.Selection.Dismiss)
                    )
                }

                is SearchScreenEvent.ConfirmAlert -> {
                    _state.update { state ->
                        state.copy(
                            alerts = state.alerts - event.alert
                        )
                    }
                    emitEventUseCase.invoke(
                        event.alert.pluginId,
                        AlertResult(NotificationEvent.Selection.Confirm)
                    )
                }

                is SearchScreenEvent.HideToast -> {
                    hideToast(event.toast.toastId)
                }

                is SearchScreenEvent.EnterAction -> {
                    executeCommandCallbackUseCase(
                        resultId = event.action.resultId,
                        callback = event.action.action.callback,
                        updateUsage = event.action.updateUsage
                    )
                }

                is SearchScreenEvent.EnterCallback -> {
                    executeCommandCallbackUseCase(
                        resultId = event.resultId,
                        callback = event.callback,
                        updateUsage = event.updateUsage
                    )
                }

                is SearchScreenEvent.ShowContextActions -> {
                    _state.update { state ->
                        state.copy(
                            contextActions = event.actions.map { action ->
                                FocusedCommandAction(
                                    resultId = event.resultId,
                                    action = action,
                                    updateUsage = false
                                )
                            },
                            showContextActions = event.actions.isNotEmpty(),
                            showActions = false
                        )
                    }
                }

                is SearchScreenEvent.FocusPluginItem -> {
                    focusFullscreenItem(event.itemId)
                }

                is SearchScreenEvent.EnterPluginItem -> {
                    enterFullscreenItem(event.itemId)
                }

                SearchScreenEvent.CloseFullscreen -> {
                    val state = _state.value
                    val fullscreen = state.fullscreen ?: return@launch
                    collapseAndCloseFullscreen(fullscreen.resultId)
                }
            }
        }
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

    private suspend fun closeFullscreen(state: SearchScreenState) {
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
            state.copy(
                toasts = state.toasts + toast
            )
        }
        val autoDismissMillis = toast.toast.autoDismissMillis ?: return
        toastDismissJobs.remove(toast.toastId)?.cancel()
        toastDismissJobs[toast.toastId] = viewModelScope.launch {
            delay(autoDismissMillis)
            dismissToast(toast.toastId, cancelJob = false)
        }
    }

    private fun hideToast(event: NotificationEvent.HideToast) {
        hideToast(event.toastId)
    }

    private fun hideToast(toastId: String) {
        dismissToast(toastId, cancelJob = true)
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
        val query = fullscreen.searchFieldState.fieldState.text.toString()
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
        val query = fullscreen.searchFieldState.fieldState.text.toString()
        val model = fullscreen.content.pluginFocusModel(itemId, query)
        val focusedItem = model.focusedItem
        val focusedActions = focusedItem?.actions
            ?.takeIf { actions -> actions.isNotEmpty() }
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
                        FocusedCommandAction(
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

    private suspend fun enterFocusedFullscreenItem(fullscreen: PluginFullscreenState) {
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

    private fun collectFullscreen(result: SearchResultSet.CommandSearchResult) {
        fullscreenJob?.cancel()
        val runtime = _state.value.plugins[result.resultId.pluginId]
        val command = runtime
            ?.manifest
            ?.commands
            ?.firstOrNull { command -> command.service == result.resultId.commandName }
        _state.update { state ->
            state.copy(
                fullscreen = PluginFullscreenState(
                    resultId = result.resultId,
                    title = result.listEntry.title,
                    placeholder = command?.placeholder?.toPluginUiText(result.resultId.pluginId),
                    searchFieldState = PresentationSearchFieldState(
                        fieldState = TextFieldState()
                    ),
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
        viewModelScope.launch {
            val focusedActions = result.actions(_state.value.plugins).map { action ->
                FocusedCommandAction(
                    resultId = result.resultId,
                    action = action,
                    updateUsage = false
                )
            }
            _state.update { state ->
                val fullscreen = state.fullscreen
                if (fullscreen?.resultId == result.resultId) {
                    state.copy(
                        focusedActions = focusedActions,
                        showActions = state.showActions && focusedActions.isNotEmpty()
                    )
                } else {
                    state
                }
            }
        }
        fullscreenJob = viewModelScope.launch {
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

data class SearchScreenState(
    val searchFieldState: PresentationSearchFieldState = PresentationSearchFieldState(
        fieldState = TextFieldState()
    ),
    val searchResults: SearchResultSet? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedActions: List<FocusedCommandAction> = emptyList(),
    val contextActions: List<FocusedCommandAction> = emptyList(),
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false,
    val showActions: Boolean = false,
    val showContextActions: Boolean = false,
    val fullscreen: PluginFullscreenState? = null,
    val alerts: List<NotificationEvent.Alert> = emptyList(),
    val toasts: List<NotificationEvent.ShowToast> = emptyList(),
    val eventSink: (SearchScreenEvent) -> Unit
)

data class PluginFullscreenState(
    val resultId: SearchResultId,
    val title: PluginUiText?,
    val placeholder: PluginUiText?,
    val searchFieldState: PresentationSearchFieldState,
    val exitBackspaceCount: Int,
    val content: List<PluginRayNodeData>,
    val focusedItemId: CommandItemId?
)

data class FocusedCommandAction(
    val resultId: SearchResultId,
    val action: PluginCommandListAction,
    val updateUsage: Boolean = true
)

private data class FullscreenQuery(
    val resultId: SearchResultId,
    val query: String
)

private data class FullscreenSearchField(
    val resultId: SearchResultId,
    val fieldState: TextFieldState
)

private fun TextFieldState.apply(state: ApiSearchFieldState) {
    edit {
        replace(0, length, state.text)
        selection = when (state.selection) {
            SearchFieldSelection.CursorAtStart -> TextRange(0)
            SearchFieldSelection.CursorAtEnd -> TextRange(state.text.length)
            SearchFieldSelection.SelectAll -> TextRange(0, state.text.length)
        }
    }
}

private fun SearchScreenState.focusedResultId(): SearchResultId? =
    focusedItemIndex?.let { itemIndex ->
        searchResults?.results?.getOrNull(itemIndex)?.resultId
    }

private suspend fun SearchResultSet?.actionsForFocused(
    index: Int?,
    plugins: Map<PluginId, PluginRuntime>
): List<FocusedCommandAction> {
    val result = index?.let { itemIndex -> this?.results?.getOrNull(itemIndex) } ?: return emptyList()
    val actions = result.actions(plugins)
    return actions.map { action ->
        FocusedCommandAction(
            resultId = result.resultId,
            action = action
        )
    }
}

private suspend fun SearchResultSet.SearchResult.actions(
    plugins: Map<PluginId, PluginRuntime>
): List<PluginCommandListAction> {
    return when (this) {
        is SearchResultSet.LiveSearchResult -> presentation.actions
        is SearchResultSet.CachedSearchResult,
        is SearchResultSet.CommandSearchResult -> plugins[resultId.pluginId]?.actions(
            commandName = resultId.commandName,
            itemId = resultId.itemId
        ).orEmpty()
    }
}

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
    data class Enter(val resultId: SearchResultId? = null) : SearchScreenEvent
    data class EnterAction(val action: FocusedCommandAction) : SearchScreenEvent
    data class EnterCallback(
        val resultId: SearchResultId,
        val callback: PluginCommandCallback,
        val updateUsage: Boolean = false
    ) : SearchScreenEvent
    data class ShowContextActions(
        val resultId: SearchResultId,
        val actions: List<PluginCommandListAction>
    ) : SearchScreenEvent
    data class FocusPluginItem(val itemId: CommandItemId) : SearchScreenEvent
    data class EnterPluginItem(val itemId: CommandItemId) : SearchScreenEvent
    data object MoveFocusPrevious : SearchScreenEvent
    data object MoveFocusNext : SearchScreenEvent
    data object ToggleActions : SearchScreenEvent
    data object HideActions : SearchScreenEvent
    data object BackspaceOnEmpty : SearchScreenEvent
    data object CloseFullscreen : SearchScreenEvent
    data class DismissAlert(val alert: NotificationEvent.Alert) : SearchScreenEvent
    data class ConfirmAlert(val alert: NotificationEvent.Alert) : SearchScreenEvent
    data class HideToast(val toast: NotificationEvent.ShowToast) : SearchScreenEvent
}
