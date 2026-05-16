package ru.raydroid.feature.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState as ApiSearchFieldState
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.BackCommandUseCase
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
import ru.raydroid.plugin.host.api.event.NotificationEvent.Alert
import ru.raydroid.plugin.host.api.event.NotificationEvent.AlertResult
import ru.raydroid.plugin.host.api.event.NotificationEvent.HideToast
import ru.raydroid.plugin.host.api.event.NotificationEvent.Selection
import ru.raydroid.plugin.host.api.event.NotificationEvent.ShowToast
import ru.raydroid.plugin.host.api.ui.PluginCommandCallback
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.api.ui.toPluginUiText

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
    private val updateCommandQueryUseCase: UpdateCommandQueryUseCase,
    private val backCommandUseCase: BackCommandUseCase? = null
) {
    constructor(
        syncCacheUseCase: SyncCacheUseCase,
        loadRuntimesUseCase: LoadRuntimesUseCase,
        searchUseCase: SearchUseCase,
        getPluginsUseCase: GetPluginsUseCase,
        openCommandUseCase: OpenCommandUseCase,
        enterItemUseCase: EnterItemUseCase,
        closeCommandUseCase: CloseCommandUseCase,
        backCommandUseCase: BackCommandUseCase,
        executeCommandCallbackUseCase: ExecuteCommandCallbackUseCase,
        getCommandFullscreenUseCase: GetCommandFullscreenUseCase,
        getEventsUseCase: GetEventsUseCase,
        emitEventUseCase: EmitEventUseCase,
        getSearchFieldRequestsUseCase: GetSearchFieldRequestsUseCase,
        updateCommandQueryUseCase: UpdateCommandQueryUseCase
    ) : this(
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        syncCacheUseCase = syncCacheUseCase,
        loadRuntimesUseCase = loadRuntimesUseCase,
        searchUseCase = searchUseCase,
        getPluginsUseCase = getPluginsUseCase,
        openCommandUseCase = openCommandUseCase,
        enterItemUseCase = enterItemUseCase,
        closeCommandUseCase = closeCommandUseCase,
        executeCommandCallbackUseCase = executeCommandCallbackUseCase,
        getCommandFullscreenUseCase = getCommandFullscreenUseCase,
        getEventsUseCase = getEventsUseCase,
        emitEventUseCase = emitEventUseCase,
        getSearchFieldRequestsUseCase = getSearchFieldRequestsUseCase,
        updateCommandQueryUseCase = updateCommandQueryUseCase,
        backCommandUseCase = backCommandUseCase
    )

    private val _state = MutableStateFlow(SearchUiState())
    private val scope = CoroutineScope(
        applicationScope.coroutineContext.minusKey(Job) + SupervisorJob()
    )
    val state = _state.asStateFlow()

    private var fullscreenJob: Job? = null
    private val toastDismissJobs = mutableMapOf<String, Job>()
    private var started = false

    init {
        start()
    }

    fun start() {
        if (started) return
        started = true

        scope.launch(Dispatchers.IO) {
            syncCacheUseCase()
        }
        scope.launch(Dispatchers.IO) {
            loadRuntimesUseCase()
        }
        scope.launch {
            getPluginsUseCase().collectLatest { plugins ->
                val pluginMap = plugins.associateBy { it.pluginId }
                val currentState = _state.value
                if (currentState.fullscreen != null) {
                    _state.update { uiState ->
                        uiState.copy(plugins = pluginMap)
                    }
                    focusFullscreenItem(currentState.fullscreen.focusedItemId)
                } else {
                    val focusedActions = currentState.searchResults.actionsForFocused(
                        currentState.focusedItemIndex,
                        pluginMap
                    )
                    _state.update { uiState ->
                        uiState.copy(
                            plugins = pluginMap,
                            focusedActions = focusedActions,
                            showActions = uiState.showActions && focusedActions.isNotEmpty()
                        )
                    }
                }
            }
        }
        scope.launch {
            getEventsUseCase().collectLatest { event ->
                when (val data = event.data) {
                    is Alert -> {
                        _state.update { uiState ->
                            uiState.copy(alerts = uiState.alerts + data)
                        }
                    }

                    is ShowToast -> {
                        showToast(data)
                    }

                    is HideToast -> {
                        hideToast(data.toastId)
                    }
                }
            }
        }
        scope.launch {
            _state
                .map { uiState -> uiState.searchFieldState.query }
                .distinctUntilChanged()
                .onEach {
                    _state.update { uiState ->
                        uiState.copy(
                            isSearching = uiState.searchResults == null,
                            focusedItemIndex = uiState.searchResults
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
                        _state.update { uiState ->
                            uiState.copy(
                                searchResults = searchResults,
                                focusedItemIndex = focusedIndex,
                                isSearching = false,
                                focusedActions = focusedActions,
                                showActions = uiState.showActions && focusedActions.isNotEmpty(),
                                contextActions = emptyList(),
                                showContextActions = false
                            )
                        }
                    }
                }
        }
        scope.launch {
            _state
                .map { uiState ->
                    uiState.fullscreen?.let { fullscreen ->
                        FullscreenQuery(fullscreen.resultId, fullscreen.searchFieldState.query)
                    }
                }
                .distinctUntilChanged()
                .onEach { fullscreenQuery ->
                    if (fullscreenQuery?.query?.isNotEmpty() == true) {
                        _state.update { uiState ->
                            val fullscreen = uiState.fullscreen ?: return@update uiState
                            if (fullscreen.resultId != fullscreenQuery.resultId) {
                                uiState
                            } else {
                                uiState.copy(
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
                _state.update { uiState ->
                    val currentFullscreen = uiState.fullscreen ?: return@update uiState
                    if (currentFullscreen.resultId.pluginId != request.pluginId) {
                        uiState
                    } else {
                        uiState.copy(
                            fullscreen = currentFullscreen.copy(
                                searchFieldState = currentFullscreen.searchFieldState.apply(request.state),
                                exitBackspaceCount = 0
                            )
                        )
                    }
                }
            }
        }
    }

    fun currentState(): SearchUiState = _state.value

    fun send(intent: SearchIntent) {
        scope.launch {
            handleIntent(intent)
        }
    }

    fun openSearch(query: String) {
        _state.update { uiState ->
            uiState.copy(
                searchFieldState = SearchFieldUiState(
                    query = query,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun openCommand(query: String) {
        _state.update { uiState ->
            uiState.copy(
                searchFieldState = SearchFieldUiState(
                    query = query,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun toggleActions() {
        _state.update { uiState ->
            uiState.copy(
                showActions = !uiState.showActions,
                showContextActions = false
            )
        }
    }

    fun hideActions() {
        _state.update { uiState ->
            uiState.copy(
                showActions = false,
                showContextActions = false
            )
        }
    }

    fun hideToast(toastId: String) {
        dismissToast(toastId, cancelJob = true)
    }

    fun confirmAlert(alert: Alert) {
        send(SearchIntent.ConfirmAlert(alert))
    }

    private suspend fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> {
                _state.update { uiState ->
                    if (uiState.fullscreen != null) {
                        uiState.copy(
                            fullscreen = uiState.fullscreen.copy(
                                searchFieldState = uiState.fullscreen.searchFieldState.copy(
                                    query = intent.query,
                                    selection = intent.selection
                                )
                            )
                        )
                    } else {
                        uiState.copy(
                            searchFieldState = uiState.searchFieldState.copy(
                                query = intent.query,
                                selection = intent.selection
                            )
                        )
                    }
                }
            }

            is SearchIntent.OpenSearch -> {
                _state.update { uiState ->
                    uiState.copy(
                        searchFieldState = SearchFieldUiState(
                            query = intent.query,
                            selection = SearchFieldSelection.CursorAtEnd
                        )
                    )
                }
            }

            is SearchIntent.OpenCommand -> {
                _state.update { uiState ->
                    uiState.copy(
                        searchFieldState = SearchFieldUiState(
                            query = intent.query,
                            selection = SearchFieldSelection.CursorAtEnd
                        )
                    )
                }
            }

            is SearchIntent.Submit -> {
                val uiState = _state.value
                val fullscreen = uiState.fullscreen
                if (fullscreen != null) {
                    enterFocusedFullscreenItem(fullscreen)
                    return
                }
                val openResultId = intent.resultId ?: uiState.focusedResultId()
                if (openResultId == null) {
                    return
                }
                val openResult = uiState.searchResults
                    ?.results
                    ?.firstOrNull { result -> result.resultId == openResultId }
                when (openResult) {
                    is SearchResultSet.CommandSearchResult -> {
                        collectFullscreen(openResult)
                        openCommandUseCase(openResultId)
                    }

                    is SearchResultSet.CachedSearchResult -> {
                        collectFullscreen(
                            result = openResult,
                            fullscreenResultId = openResultId.copy(itemId = CommandItemId.CommandRoot)
                        )
                        enterItemUseCase(openResultId)
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

                    null -> Unit
                }
            }

            SearchIntent.MoveFocusNext -> {
                val uiState = _state.value
                if (uiState.fullscreen != null) {
                    moveFullscreenFocus(1)
                    return
                }
                val focusedIndex = uiState.searchResults?.let { searchResults ->
                    uiState.focusedItemIndex?.let { itemIndex ->
                        (itemIndex + 1).coerceIn(0, searchResults.results.lastIndex)
                    }
                }
                val focusedActions = uiState.searchResults.actionsForFocused(
                    focusedIndex,
                    uiState.plugins
                )
                _state.update { currentState ->
                    currentState.copy(
                        focusedItemIndex = focusedIndex,
                        focusedActions = focusedActions,
                        showActions = false,
                        contextActions = emptyList(),
                        showContextActions = false
                    )
                }
            }

            SearchIntent.MoveFocusPrevious -> {
                val uiState = _state.value
                if (uiState.fullscreen != null) {
                    moveFullscreenFocus(-1)
                    return
                }
                val focusedIndex = uiState.searchResults?.let { searchResults ->
                    uiState.focusedItemIndex?.let { itemIndex ->
                        (itemIndex - 1).coerceIn(0, searchResults.results.lastIndex)
                    }
                }
                val focusedActions = uiState.searchResults.actionsForFocused(
                    focusedIndex,
                    uiState.plugins
                )
                _state.update { currentState ->
                    currentState.copy(
                        focusedItemIndex = focusedIndex,
                        focusedActions = focusedActions,
                        showActions = false,
                        contextActions = emptyList(),
                        showContextActions = false
                    )
                }
            }

            SearchIntent.ToggleActions -> {
                _state.update { uiState ->
                    uiState.copy(
                        showActions = !uiState.showActions,
                        showContextActions = false
                    )
                }
            }

            SearchIntent.HideActions -> {
                _state.update { uiState ->
                    uiState.copy(
                        showActions = false,
                        showContextActions = false
                    )
                }
            }

            SearchIntent.BackspaceOnEmpty -> {
                val uiState = _state.value
                val fullscreen = uiState.fullscreen ?: return
                if (backCommandUseCase?.invoke(fullscreen.resultId) == true) {
                    _state.update { currentState ->
                        val currentFullscreen = currentState.fullscreen ?: return@update currentState
                        if (currentFullscreen.resultId == fullscreen.resultId) {
                            currentState.copy(
                                fullscreen = currentFullscreen.copy(exitBackspaceCount = 0)
                            )
                        } else {
                            currentState
                        }
                    }
                    return
                }
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

            SearchIntent.CloseFullscreen -> {
                val uiState = _state.value
                val fullscreen = uiState.fullscreen ?: return
                if (backCommandUseCase?.invoke(fullscreen.resultId) == true) {
                    return
                }
                collapseAndCloseFullscreen(fullscreen.resultId)
            }

            is SearchIntent.EnterAction -> {
                executeCommandCallbackUseCase(
                    resultId = intent.action.resultId,
                    callback = intent.action.action.callback,
                    updateUsage = intent.action.updateUsage
                )
            }

            is SearchIntent.EnterCallback -> {
                executeCommandCallbackUseCase(
                    resultId = intent.resultId,
                    callback = intent.callback,
                    updateUsage = intent.updateUsage
                )
            }

            is SearchIntent.ShowContextActions -> {
                _state.update { uiState ->
                    uiState.copy(
                        contextActions = intent.actions.map { action ->
                            FocusedCommandAction(
                                resultId = intent.resultId,
                                action = action,
                                updateUsage = false
                            )
                        },
                        showContextActions = intent.actions.isNotEmpty(),
                        showActions = false
                    )
                }
            }

            is SearchIntent.FocusPluginItem -> {
                focusFullscreenItem(intent.itemId)
            }

            is SearchIntent.EnterPluginItem -> {
                enterFullscreenItem(intent.itemId)
            }

            is SearchIntent.DismissAlert -> {
                _state.update { uiState ->
                    uiState.copy(alerts = uiState.alerts - intent.alert)
                }
                emitEventUseCase.invoke(
                    intent.alert.pluginId,
                    AlertResult(Selection.Dismiss)
                )
            }

            is SearchIntent.ConfirmAlert -> {
                _state.update { uiState ->
                    uiState.copy(alerts = uiState.alerts - intent.alert)
                }
                emitEventUseCase.invoke(
                    intent.alert.pluginId,
                    AlertResult(Selection.Confirm)
                )
            }

            is SearchIntent.DismissToast -> {
                dismissToast(intent.toastId, cancelJob = true)
            }
        }
    }

    private suspend fun collapseAndCloseFullscreen(resultId: SearchResultId) {
        _state.update { uiState ->
            val fullscreen = uiState.fullscreen ?: return@update uiState
            if (fullscreen.resultId != resultId) {
                uiState
            } else {
                uiState.copy(
                    fullscreen = fullscreen.copy(exitBackspaceCount = 2)
                )
            }
        }
        delay(FULLSCREEN_COLLAPSE_DELAY_MS)
        val uiState = _state.value
        if (uiState.fullscreen?.resultId == resultId) {
            closeFullscreen(uiState)
        }
    }

    private suspend fun closeFullscreen(uiState: SearchUiState) {
        val fullscreen = uiState.fullscreen ?: return
        fullscreenJob?.cancel()
        fullscreenJob = null
        closeCommandUseCase(fullscreen.resultId)
        _state.update { currentState ->
            currentState.copy(fullscreen = null)
        }
    }

    private fun showToast(toast: ShowToast) {
        val replacedToastIds = _state.value.toasts
            .filter { existing -> existing.shouldBeReplacedBy(toast) }
            .map { existing -> existing.toastId }
        replacedToastIds.forEach { toastId ->
            toastDismissJobs.remove(toastId)?.cancel()
        }
        _state.update { uiState ->
            uiState.copy(
                toasts = uiState.toasts
                    .filterNot { existing -> existing.shouldBeReplacedBy(toast) } + toast
            )
        }
        val autoDismissMillis = toast.toast.autoDismissMillis ?: return
        toastDismissJobs.remove(toast.toastId)?.cancel()
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
        _state.update { uiState ->
            uiState.copy(
                toasts = uiState.toasts.filterNot { toast -> toast.toastId == toastId }
            )
        }
    }

    private suspend fun moveFullscreenFocus(delta: Int) {
        val fullscreen = _state.value.fullscreen ?: return
        val query = fullscreen.searchFieldState.query
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
        val uiState = _state.value
        val fullscreen = uiState.fullscreen ?: return
        val query = fullscreen.searchFieldState.query
        val model = fullscreen.content.pluginFocusModel(itemId, query)
        val focusedItem = model.focusedItem
        val currentFocusedActions = uiState.focusedActions
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
                    uiState.plugins[resultId.pluginId]?.actions(resultId.commandName, item.id)
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
            uiState.plugins[fullscreen.resultId.pluginId]?.update(
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
        val uiState = _state.value
        val fullscreen = uiState.fullscreen ?: return
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
            uiState.plugins[fullscreen.resultId.pluginId]?.update(
                fullscreen.resultId.commandName,
                CommandActionBridge.Regular(CommandAction.Enter(itemId))
            )
        }
    }

    private fun collectFullscreen(
        result: SearchResultSet.SearchResult,
        fullscreenResultId: SearchResultId = result.resultId
    ) {
        fullscreenJob?.cancel()
        val runtime = _state.value.plugins[fullscreenResultId.pluginId]
        val command = runtime
            ?.manifest
            ?.commands
            ?.firstOrNull { command -> command.service == fullscreenResultId.commandName }
        _state.update { uiState ->
            uiState.copy(
                fullscreen = PluginFullscreenState(
                    resultId = fullscreenResultId,
                    title = result.listEntry.title,
                    placeholder = command?.placeholder?.toPluginUiText(fullscreenResultId.pluginId),
                    searchFieldState = SearchFieldUiState(),
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
        scope.launch {
            val focusedActions = if (result.resultId == fullscreenResultId) {
                result.actions(_state.value.plugins).map { action ->
                    FocusedCommandAction(
                        resultId = fullscreenResultId,
                        action = action,
                        updateUsage = false
                    )
                }
            } else {
                emptyList()
            }
            _state.update { uiState ->
                val fullscreen = uiState.fullscreen
                if (fullscreen?.resultId == fullscreenResultId) {
                    val query = fullscreen.searchFieldState.query
                    if (fullscreen.content.pluginFocusModel(fullscreen.focusedItemId, query).focusedItemId != null) {
                        uiState
                    } else {
                        uiState.copy(
                            focusedActions = focusedActions,
                            showActions = uiState.showActions && focusedActions.isNotEmpty()
                        )
                    }
                } else {
                    uiState
                }
            }
        }
        fullscreenJob = scope.launch {
            getCommandFullscreenUseCase(fullscreenResultId).collectLatest { content ->
                val newContent = content?.content.orEmpty()
                _state.update { uiState ->
                    val fullscreen = uiState.fullscreen
                    if (fullscreen?.resultId != fullscreenResultId) {
                        uiState
                    } else {
                        uiState.copy(
                            fullscreen = fullscreen.copy(content = newContent)
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
    val searchFieldState: SearchFieldUiState = SearchFieldUiState(),
    val searchResults: SearchResultSet? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedActions: List<FocusedCommandAction> = emptyList(),
    val contextActions: List<FocusedCommandAction> = emptyList(),
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false,
    val showActions: Boolean = false,
    val showContextActions: Boolean = false,
    val fullscreen: PluginFullscreenState? = null,
    val alerts: List<Alert> = emptyList(),
    val toasts: List<ShowToast> = emptyList()
)

data class SearchFieldUiState(
    val query: String = "",
    val selection: SearchFieldSelection = SearchFieldSelection.CursorAtEnd
) {
    fun apply(state: ApiSearchFieldState): SearchFieldUiState {
        return copy(
            query = state.text,
            selection = state.selection
        )
    }
}

data class PluginFullscreenState(
    val resultId: SearchResultId,
    val title: PluginUiText?,
    val placeholder: PluginUiText?,
    val searchFieldState: SearchFieldUiState,
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

sealed interface SearchIntent {
    data class UpdateQuery(
        val query: String,
        val selection: SearchFieldSelection = SearchFieldSelection.CursorAtEnd
    ) : SearchIntent

    data class OpenSearch(val query: String = "") : SearchIntent
    data class OpenCommand(val query: String) : SearchIntent
    data class Submit(val resultId: SearchResultId? = null) : SearchIntent
    data class EnterAction(val action: FocusedCommandAction) : SearchIntent
    data class EnterCallback(
        val resultId: SearchResultId,
        val callback: PluginCommandCallback,
        val updateUsage: Boolean = false
    ) : SearchIntent

    data class ShowContextActions(
        val resultId: SearchResultId,
        val actions: List<PluginCommandListAction>
    ) : SearchIntent

    data class FocusPluginItem(val itemId: CommandItemId) : SearchIntent
    data class EnterPluginItem(val itemId: CommandItemId) : SearchIntent
    data object MoveFocusPrevious : SearchIntent
    data object MoveFocusNext : SearchIntent
    data object ToggleActions : SearchIntent
    data object HideActions : SearchIntent
    data object BackspaceOnEmpty : SearchIntent
    data object CloseFullscreen : SearchIntent
    data class DismissAlert(val alert: Alert) : SearchIntent
    data class ConfirmAlert(val alert: Alert) : SearchIntent
    data class DismissToast(val toastId: String) : SearchIntent
}

private fun ShowToast.shouldBeReplacedBy(incoming: ShowToast): Boolean =
    toastId == incoming.toastId || matchesAnimatedLoadingToast(incoming)

private fun ShowToast.matchesAnimatedLoadingToast(incoming: ShowToast): Boolean =
    toast.style == NotificationEvent.Toast.Style.Animated &&
        incoming.toast.style == NotificationEvent.Toast.Style.Animated &&
        toast.message == incoming.toast.message

private fun SearchUiState.focusedResultId(): SearchResultId? =
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
