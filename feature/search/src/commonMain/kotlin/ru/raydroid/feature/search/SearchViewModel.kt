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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState as ApiSearchFieldState
import ru.raydroid.plugin.api.manifest.Command
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
import ru.raydroid.plugin.host.api.application.usecase.RemoveSearchAliasUseCase
import ru.raydroid.plugin.host.api.application.usecase.SaveSearchAliasUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.application.usecase.UpdateCommandQueryUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchAliasInvalidReason
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
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
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.pluginFocusModel
import ru.raydroid.plugin.host.api.ui.toPluginUiText

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
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
    private val saveSearchAliasUseCase: SaveSearchAliasUseCase,
    private val removeSearchAliasUseCase: RemoveSearchAliasUseCase,
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
        updateCommandQueryUseCase: UpdateCommandQueryUseCase,
        saveSearchAliasUseCase: SaveSearchAliasUseCase,
        removeSearchAliasUseCase: RemoveSearchAliasUseCase
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
        saveSearchAliasUseCase = saveSearchAliasUseCase,
        removeSearchAliasUseCase = removeSearchAliasUseCase,
        backCommandUseCase = backCommandUseCase
    )

    private val backingState = MutableStateFlow(SearchViewModelState())
    private val scope = CoroutineScope(
        applicationScope.coroutineContext.minusKey(Job) + SupervisorJob()
    )
    val state = backingState
        .map { it.toScreenState() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = backingState.value.toScreenState()
        )

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
                val currentState = backingState.value
                if (currentState.fullscreen != null) {
                    backingState.update { uiState ->
                        uiState.copy(plugins = pluginMap)
                    }
                    focusFullscreenItem(currentState.fullscreen.focusedItemId)
                } else {
                    val focusedActions = currentState.searchResults.actionsForFocused(
                        currentState.focusedItemIndex,
                        pluginMap
                    )
                    backingState.update { uiState ->
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
                        backingState.update { uiState ->
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
            backingState
                .map { uiState -> uiState.searchFieldState.query }
                .distinctUntilChanged()
                .onEach {
                    backingState.update { uiState ->
                        uiState.copy(
                            isSearching = uiState.searchResults == null,
                            focusedItemIndex = uiState.searchResults
                                ?.results
                                ?.takeIf { results -> results.isNotEmpty() }
                                ?.let { 0 },
                            focusedActions = emptyList(),
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false,
                            activeContextSourceId = null
                        )
                    }
                }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query ->
                    var focusInitialized = false
                    searchUseCase(query).collectLatest { searchResults ->
                        val currentState = backingState.value
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
                        backingState.update { uiState ->
                            uiState.copy(
                                searchResults = searchResults,
                                focusedItemIndex = focusedIndex,
                                isSearching = false,
                                focusedActions = focusedActions,
                                showActions = uiState.showActions && focusedActions.isNotEmpty(),
                                contextActions = emptyList(),
                                showContextActions = false,
                                activeContextSourceId = null
                            )
                        }
                    }
                }
        }
        scope.launch {
            backingState
                .map { uiState ->
                    uiState.fullscreen?.let { fullscreen ->
                        FullscreenQuery(fullscreen.resultId, fullscreen.searchFieldState.query)
                    }
                }
                .distinctUntilChanged()
                .onEach { fullscreenQuery ->
                    if (fullscreenQuery?.query?.isNotEmpty() == true) {
                        backingState.update { uiState ->
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
                        focusFullscreenItem(backingState.value.fullscreen?.focusedItemId)
                    }
                }
        }
        scope.launch {
            getSearchFieldRequestsUseCase().collectLatest { request ->
                val fullscreen = backingState.value.fullscreen ?: return@collectLatest
                if (fullscreen.resultId.pluginId != request.pluginId) {
                    return@collectLatest
                }
                backingState.update { uiState ->
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

    fun currentState(): SearchScreenState = backingState.value.toScreenState()

    fun onEvent(event: SearchScreenEvent) {
        scope.launch {
            dispatchEvent(event)
        }
    }

    internal suspend fun dispatchEvent(event: SearchScreenEvent) {
        handleEvent(event)
    }

    fun openSearch(query: String) {
        backingState.update { uiState ->
            uiState.copy(
                searchFieldState = SearchFieldUiState(
                    query = query,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun openCommand(query: String) {
        backingState.update { uiState ->
            uiState.copy(
                searchFieldState = SearchFieldUiState(
                    query = query,
                    selection = SearchFieldSelection.CursorAtEnd
                )
            )
        }
    }

    fun toggleActions() {
        backingState.update { uiState ->
            uiState.copy(
                showActions = !uiState.showActions,
                showContextActions = false,
                activeContextSourceId = null
            )
        }
    }

    fun hideActions() {
        backingState.update { uiState ->
            uiState.copy(
                showActions = false,
                showContextActions = false,
                activeContextSourceId = null
            )
        }
    }

    fun hideToast(toastId: String) {
        dismissToast(toastId, cancelJob = true)
    }

    fun confirmAlert(alert: Alert) {
        onEvent(SearchScreenEvent.ConfirmAlert(alert))
    }

    private suspend fun handleEvent(event: SearchScreenEvent) {
        when (event) {
            is SearchScreenEvent.UpdateQuery -> {
                backingState.update { uiState ->
                    if (uiState.fullscreen != null) {
                        uiState.copy(
                            fullscreen = uiState.fullscreen.copy(
                                searchFieldState = uiState.fullscreen.searchFieldState.copy(
                                    query = event.query,
                                    selection = event.selection
                                )
                            )
                        )
                    } else {
                        uiState.copy(
                            searchFieldState = uiState.searchFieldState.copy(
                                query = event.query,
                                selection = event.selection
                            )
                        )
                    }
                }
            }

            is SearchScreenEvent.OpenSearch -> {
                backingState.update { uiState ->
                    uiState.copy(
                        searchFieldState = SearchFieldUiState(
                            query = event.query,
                            selection = SearchFieldSelection.CursorAtEnd
                        )
                    )
                }
            }

            is SearchScreenEvent.OpenCommand -> {
                backingState.update { uiState ->
                    uiState.copy(
                        searchFieldState = SearchFieldUiState(
                            query = event.query,
                            selection = SearchFieldSelection.CursorAtEnd
                        )
                    )
                }
            }

            is SearchScreenEvent.Submit -> {
                val uiState = backingState.value
                val fullscreen = uiState.fullscreen
                if (fullscreen != null) {
                    enterFocusedFullscreenItem(fullscreen)
                    return
                }
                val openResultId = event.resultId ?: uiState.focusedResultId()
                if (openResultId == null) {
                    return
                }
                val openResult = uiState.searchResults
                    ?.results
                    ?.firstOrNull { result -> result.resultId == openResultId }
                when (openResult) {
                    is SearchResultSet.CommandSearchResult -> {
                        if (openResultId.commandMode() != Command.Mode.NoView) {
                            collectFullscreen(openResult)
                        }
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
                            if (openResult.presentation.content.isEmpty()) {
                                collectFullscreenWhenContentAppears(
                                    result = openResult,
                                    fullscreenResultId = openResultId.copy(itemId = CommandItemId.CommandRoot)
                                )
                            }
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

            SearchScreenEvent.MoveFocusNext -> {
                val uiState = backingState.value
                if (uiState.fullscreen != null) {
                    moveFullscreenFocus(1)
                    return
                }
                val focusedIndex = uiState.searchResults?.let { searchResults ->
                    nextSearchResultsFocusIndex(
                        currentIndex = uiState.focusedItemIndex,
                        resultCount = searchResults.results.size
                    )
                }
                val focusedActions = uiState.searchResults.actionsForFocused(
                    focusedIndex,
                    uiState.plugins
                )
                backingState.update { currentState ->
                        currentState.copy(
                            focusedItemIndex = focusedIndex,
                            focusedActions = focusedActions,
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false,
                            activeContextSourceId = null
                        )
                }
            }

            SearchScreenEvent.MoveFocusPrevious -> {
                val uiState = backingState.value
                if (uiState.fullscreen != null) {
                    moveFullscreenFocus(-1)
                    return
                }
                val focusedIndex = uiState.searchResults?.let { searchResults ->
                    previousSearchResultsFocusIndex(
                        currentIndex = uiState.focusedItemIndex,
                        resultCount = searchResults.results.size
                    )
                }
                val focusedActions = uiState.searchResults.actionsForFocused(
                    focusedIndex,
                    uiState.plugins
                )
                backingState.update { currentState ->
                        currentState.copy(
                            focusedItemIndex = focusedIndex,
                            focusedActions = focusedActions,
                            showActions = false,
                            contextActions = emptyList(),
                            showContextActions = false,
                            activeContextSourceId = null
                        )
                }
            }

            SearchScreenEvent.ToggleActions -> {
                backingState.update { uiState ->
                    uiState.copy(
                        showActions = !uiState.showActions,
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
            }

            SearchScreenEvent.HideActions -> {
                backingState.update { uiState ->
                    uiState.copy(
                        showActions = false,
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
            }

            SearchScreenEvent.BackspaceOnEmpty -> {
                val uiState = backingState.value
                val fullscreen = uiState.fullscreen ?: return
                backingState.update { currentState ->
                    currentState.copy(
                        contextActions = emptyList(),
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
                if (backCommandUseCase?.invoke(fullscreen.resultId) == true) {
                    backingState.update { currentState ->
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
                backingState.update { currentState ->
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

            SearchScreenEvent.CloseFullscreen -> {
                val uiState = backingState.value
                val fullscreen = uiState.fullscreen ?: return
                backingState.update { currentState ->
                    currentState.copy(
                        contextActions = emptyList(),
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
                if (backCommandUseCase?.invoke(fullscreen.resultId) == true) {
                    return
                }
                collapseAndCloseFullscreen(fullscreen.resultId)
            }

            is SearchScreenEvent.EnterAction -> {
                when (val kind = event.action.action.kind) {
                    is SearchPanelAction.Kind.PluginCallback -> {
                        backingState.update { uiState ->
                            uiState.copy(
                                showActions = false,
                                contextActions = emptyList(),
                                showContextActions = false,
                                activeContextSourceId = null
                            )
                        }
                        executeCommandCallbackUseCase(
                            resultId = event.action.resultId,
                            callback = kind.callback,
                            updateUsage = kind.updateUsage && event.action.updateUsage
                        )
                    }

                    is SearchPanelAction.Kind.OpenAliasEditor -> {
                        backingState.update { uiState ->
                            uiState.copy(
                                aliasEditor = SearchAliasEditorState(
                                    resultId = event.action.resultId,
                                    title = currentResultTitle(event.action.resultId),
                                    input = kind.existingAlias.orEmpty(),
                                    existingAlias = kind.existingAlias
                                ),
                                showActions = false,
                                showContextActions = false,
                                activeContextSourceId = null
                            )
                        }
                    }

                    SearchPanelAction.Kind.RemoveAlias -> {
                        removeSearchAliasUseCase(event.action.resultId)
                        backingState.update { uiState ->
                            uiState.copy(
                                aliasEditor = uiState.aliasEditor
                                    ?.takeUnless { editor -> editor.resultId == event.action.resultId },
                                showActions = false,
                                showContextActions = false,
                                activeContextSourceId = null
                            )
                        }
                    }
                }
            }

            is SearchScreenEvent.EnterQuickAction -> {
                enterQuickAction(event.resultId)
            }

            is SearchScreenEvent.EnterCallback -> {
                backingState.update { uiState ->
                    uiState.copy(
                        showActions = false,
                        contextActions = emptyList(),
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
                executeCommandCallbackUseCase(
                    resultId = event.resultId,
                    callback = event.callback,
                    updateUsage = event.updateUsage
                )
            }

            is SearchScreenEvent.ShowContextActions -> {
                backingState.update { uiState ->
                    uiState.copy(
                        contextActions = event.actions.toSearchPanelActions(updateUsage = false).map { action ->
                            FocusedCommandAction(
                                resultId = event.resultId,
                                action = action,
                                updateUsage = false
                            )
                        },
                        showContextActions = event.actions.isNotEmpty(),
                        showActions = false,
                        activeContextSourceId = event.sourceId.takeIf { event.actions.isNotEmpty() }
                    )
                }
            }

            is SearchScreenEvent.ShowResultContextActions -> {
                val currentState = backingState.value
                val result = currentState.searchResults
                    ?.results
                    ?.firstOrNull { searchResult -> searchResult.resultId == event.resultId }
                    ?: return
                val actions = result.actions(currentState.plugins)
                if (actions.isEmpty()) {
                    return
                }
                backingState.update { uiState ->
                    uiState.copy(
                        contextActions = actions.map { action ->
                            FocusedCommandAction(
                                resultId = event.resultId,
                                action = action,
                                updateUsage = false
                            )
                        },
                        showContextActions = true,
                        showActions = false,
                        activeContextSourceId = event.sourceId
                    )
                }
            }

            is SearchScreenEvent.UpdateAliasEditorInput -> {
                backingState.update { uiState ->
                    uiState.copy(
                        aliasEditor = uiState.aliasEditor?.copy(
                            input = event.value,
                            error = null
                        )
                    )
                }
            }

            SearchScreenEvent.SaveAliasEditor -> {
                val editor = backingState.value.aliasEditor ?: return
                when (val result = saveSearchAliasUseCase(editor.resultId, editor.input)) {
                    is SearchAliasSaveResult.Success -> {
                        backingState.update { uiState ->
                            uiState.copy(aliasEditor = null)
                        }
                    }

                    is SearchAliasSaveResult.Conflict -> {
                        backingState.update { uiState ->
                            uiState.copy(
                                aliasEditor = uiState.aliasEditor?.copy(
                                    error = PluginUiText.Plain("Alias '${result.alias}' is already in use")
                                )
                            )
                        }
                    }

                    is SearchAliasSaveResult.Invalid -> {
                        backingState.update { uiState ->
                            uiState.copy(
                                aliasEditor = uiState.aliasEditor?.copy(
                                    error = PluginUiText.Plain(result.reason.message())
                                )
                            )
                        }
                    }
                }
            }

            SearchScreenEvent.RemoveAlias -> {
                val editor = backingState.value.aliasEditor ?: return
                removeSearchAliasUseCase(editor.resultId)
                backingState.update { uiState ->
                    uiState.copy(aliasEditor = null)
                }
            }

            SearchScreenEvent.DismissAliasEditor -> {
                backingState.update { uiState ->
                    uiState.copy(aliasEditor = null)
                }
            }

            is SearchScreenEvent.FocusPluginItem -> {
                focusFullscreenItem(event.itemId)
            }

            is SearchScreenEvent.EnterPluginItem -> {
                enterFullscreenItem(event.itemId)
            }

            is SearchScreenEvent.DismissAlert -> {
                backingState.update { uiState ->
                    uiState.copy(
                        alerts = uiState.alerts - event.alert,
                        contextActions = emptyList(),
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
                emitEventUseCase.invoke(
                    event.alert.pluginId,
                    AlertResult(Selection.Dismiss)
                )
            }

            is SearchScreenEvent.ConfirmAlert -> {
                backingState.update { uiState ->
                    uiState.copy(
                        alerts = uiState.alerts - event.alert,
                        contextActions = emptyList(),
                        showContextActions = false,
                        activeContextSourceId = null
                    )
                }
                emitEventUseCase.invoke(
                    event.alert.pluginId,
                    AlertResult(Selection.Confirm)
                )
            }

            is SearchScreenEvent.DismissToast -> {
                dismissToast(event.toastId, cancelJob = true)
            }
        }
    }

    private fun SearchResultId.commandMode(): Command.Mode? {
        val runtime = backingState.value.plugins[pluginId] ?: return null
        return runtime.manifest.commands
            .firstOrNull { command -> command.service == commandName }
            ?.mode
    }

    private suspend fun collapseAndCloseFullscreen(resultId: SearchResultId) {
        backingState.update { uiState ->
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
        val uiState = backingState.value
        if (uiState.fullscreen?.resultId == resultId) {
            closeFullscreen(uiState)
        }
    }

    private suspend fun closeFullscreen(uiState: SearchViewModelState) {
        val fullscreen = uiState.fullscreen ?: return
        fullscreenJob?.cancel()
        fullscreenJob = null
        closeCommandUseCase(fullscreen.resultId)
        val focusedActions = uiState.searchResults.actionsForFocused(
            uiState.focusedItemIndex,
            uiState.plugins
        )
        backingState.update { currentState ->
            currentState.copy(
                fullscreen = null,
                focusedActions = focusedActions,
                showActions = currentState.showActions && focusedActions.isNotEmpty(),
                contextActions = emptyList(),
                showContextActions = false,
                activeContextSourceId = null
            )
        }
    }

    private fun currentResultTitle(resultId: SearchResultId): PluginUiText? {
        return backingState.value.searchResults
            ?.results
            ?.firstOrNull { result -> result.resultId == resultId }
            ?.listEntry
            ?.title
    }

    private fun showToast(toast: ShowToast) {
        val replacedToastIds = backingState.value.toasts
            .filter { existing -> existing.shouldBeReplacedBy(toast) }
            .map { existing -> existing.toastId }
        replacedToastIds.forEach { toastId ->
            toastDismissJobs.remove(toastId)?.cancel()
        }
        backingState.update { uiState ->
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
        backingState.update { uiState ->
            uiState.copy(
                toasts = uiState.toasts.filterNot { toast -> toast.toastId == toastId }
            )
        }
    }

    private suspend fun moveFullscreenFocus(delta: Int) {
        val fullscreen = backingState.value.fullscreen ?: return
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
        val uiState = backingState.value
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
            ?.toSearchPanelActions(updateUsage = false)
            ?: currentFocusedActions.takeIf { actions -> actions.isNotEmpty() }
            ?: fullscreen.resultId.let { resultId ->
                focusedItem?.let { item ->
                    uiState.plugins[resultId.pluginId]?.actions(resultId.commandName, item.id)
                }?.toSearchPanelActions(updateUsage = false)
            }.orEmpty()
        backingState.update { currentState ->
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
                    showContextActions = false,
                    activeContextSourceId = null
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

    private suspend fun enterFocusedFullscreenItem(fullscreen: SearchFullscreenContentState) {
        val itemId = fullscreen.focusedItemId ?: return
        enterFullscreenItem(itemId)
    }

    private suspend fun enterQuickAction(resultId: SearchResultId) {
        val uiState = backingState.value
        val result = uiState.searchResults
            ?.results
            ?.firstOrNull { result -> result.resultId == resultId }
            ?: return
        val action = result.actions(uiState.plugins)
            .firstOrNull { action -> action.primary }
            ?: return
        handleEvent(
            SearchScreenEvent.EnterAction(
                FocusedCommandAction(
                    resultId = resultId,
                    action = action
                )
            )
        )
    }

    private suspend fun enterFullscreenItem(itemId: CommandItemId) {
        val uiState = backingState.value
        val fullscreen = uiState.fullscreen ?: return
        focusFullscreenItem(itemId)
        val focusedAction = backingState.value.focusedActions
            .firstOrNull { action -> action.action.primary }
            ?: backingState.value.focusedActions.firstOrNull()
        if (focusedAction != null) {
            when (val kind = focusedAction.action.kind) {
                is SearchPanelAction.Kind.PluginCallback -> {
                    executeCommandCallbackUseCase(
                        resultId = focusedAction.resultId,
                        callback = kind.callback,
                        updateUsage = false
                    )
                }

                is SearchPanelAction.Kind.OpenAliasEditor,
                SearchPanelAction.Kind.RemoveAlias -> Unit
            }
        } else {
            uiState.plugins[fullscreen.resultId.pluginId]?.update(
                fullscreen.resultId.commandName,
                CommandActionBridge.Regular(CommandAction.Enter(itemId))
            )
        }
    }

    private fun collectFullscreenWhenContentAppears(
        result: SearchResultSet.SearchResult,
        fullscreenResultId: SearchResultId
    ) {
        fullscreenJob?.cancel()
        val runtime = backingState.value.plugins[fullscreenResultId.pluginId]
        val command = runtime
            ?.manifest
            ?.commands
            ?.firstOrNull { command -> command.service == fullscreenResultId.commandName }
        fullscreenJob = scope.launch {
            getCommandFullscreenUseCase(fullscreenResultId).collectLatest { content ->
                val newContent = content?.content.orEmpty()
                if (newContent.isEmpty()) return@collectLatest
                backingState.update { uiState ->
                    uiState.copy(
                        fullscreen = SearchFullscreenContentState(
                            resultId = fullscreenResultId,
                            title = result.listEntry.title,
                            placeholder = command?.placeholder?.toPluginUiText(fullscreenResultId.pluginId),
                            searchFieldState = SearchFieldUiState(),
                            exitBackspaceCount = 0,
                            content = newContent,
                            focusedItemId = null
                        ),
                        focusedActions = emptyList(),
                        showActions = false,
                        contextActions = emptyList(),
                        showContextActions = false
                    )
                }
                focusFullscreenItem(backingState.value.fullscreen?.focusedItemId)
            }
        }
    }

    private fun collectFullscreen(
        result: SearchResultSet.SearchResult,
        fullscreenResultId: SearchResultId = result.resultId
    ) {
        fullscreenJob?.cancel()
        val runtime = backingState.value.plugins[fullscreenResultId.pluginId]
        val command = runtime
            ?.manifest
            ?.commands
            ?.firstOrNull { command -> command.service == fullscreenResultId.commandName }
        backingState.update { uiState ->
            uiState.copy(
                fullscreen = SearchFullscreenContentState(
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
                showContextActions = false,
                activeContextSourceId = null
            )
        }
        scope.launch {
            val focusedActions = if (result.resultId == fullscreenResultId) {
                result.actions(backingState.value.plugins).map { action ->
                    FocusedCommandAction(
                        resultId = fullscreenResultId,
                        action = action,
                        updateUsage = false
                    )
                }
            } else {
                emptyList()
            }
            backingState.update { uiState ->
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
                backingState.update { uiState ->
                    val fullscreen = uiState.fullscreen
                    if (fullscreen?.resultId != fullscreenResultId) {
                        uiState
                    } else {
                        uiState.copy(
                            fullscreen = fullscreen.copy(content = newContent)
                        )
                    }
                }
                focusFullscreenItem(backingState.value.fullscreen?.focusedItemId)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 24L
        const val FULLSCREEN_COLLAPSE_DELAY_MS = 160L
    }
}

private data class SearchViewModelState(
    val searchFieldState: SearchFieldUiState = SearchFieldUiState(),
    val searchResults: SearchResultSet? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedActions: List<FocusedCommandAction> = emptyList(),
    val contextActions: List<FocusedCommandAction> = emptyList(),
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false,
    val showActions: Boolean = false,
    val showContextActions: Boolean = false,
    val activeContextSourceId: String? = null,
    val aliasEditor: SearchAliasEditorState? = null,
    val fullscreen: SearchFullscreenContentState? = null,
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

data class SearchFullscreenContentState(
    val resultId: SearchResultId,
    val title: PluginUiText?,
    val placeholder: PluginUiText?,
    val searchFieldState: SearchFieldUiState,
    val exitBackspaceCount: Int,
    val content: List<PluginRayNodeData>,
    val focusedItemId: CommandItemId?,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap()
)

data class SearchAliasEditorState(
    val resultId: SearchResultId,
    val title: PluginUiText?,
    val input: String,
    val existingAlias: String?,
    val error: PluginUiText? = null
)

data class FocusedCommandAction(
    val resultId: SearchResultId,
    val action: SearchPanelAction,
    val updateUsage: Boolean = true
)

private data class FullscreenQuery(
    val resultId: SearchResultId,
    val query: String
)

data class SearchOverlayState(
    val focusedActions: List<FocusedCommandAction> = emptyList(),
    val contextActions: List<FocusedCommandAction> = emptyList(),
    val showActions: Boolean = false,
    val showContextActions: Boolean = false,
    val activeContextSourceId: String? = null,
    val aliasEditor: SearchAliasEditorState? = null
)

data class SearchResultsContentState(
    val searchFieldState: SearchFieldUiState = SearchFieldUiState(),
    val searchResults: SearchResultSet? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false
)

sealed class SearchScreenState {
    abstract val searchFieldState: SearchFieldUiState
    abstract val plugins: Map<PluginId, PluginRuntime>
    abstract val overlayState: SearchOverlayState
    abstract val alerts: List<Alert>
    abstract val toasts: List<ShowToast>
    open val resultsContent: SearchResultsContentState? = null
    open val fullscreenContent: SearchFullscreenContentState? = null

    data class Loading(
        val content: SearchResultsContentState = SearchResultsContentState(isSearching = true),
        override val overlayState: SearchOverlayState = SearchOverlayState(),
        override val alerts: List<Alert> = emptyList(),
        override val toasts: List<ShowToast> = emptyList()
    ) : SearchScreenState() {
        override val searchFieldState: SearchFieldUiState
            get() = content.searchFieldState
        override val plugins: Map<PluginId, PluginRuntime>
            get() = content.plugins
        override val resultsContent: SearchResultsContentState
            get() = content
    }

    data class Results(
        val content: SearchResultsContentState,
        override val overlayState: SearchOverlayState = SearchOverlayState(),
        override val alerts: List<Alert> = emptyList(),
        override val toasts: List<ShowToast> = emptyList()
    ) : SearchScreenState() {
        override val searchFieldState: SearchFieldUiState
            get() = content.searchFieldState
        override val plugins: Map<PluginId, PluginRuntime>
            get() = content.plugins
        override val resultsContent: SearchResultsContentState
            get() = content
    }

    data class Fullscreen(
        val content: SearchFullscreenContentState,
        override val overlayState: SearchOverlayState = SearchOverlayState(),
        override val alerts: List<Alert> = emptyList(),
        override val toasts: List<ShowToast> = emptyList()
    ) : SearchScreenState() {
        override val searchFieldState: SearchFieldUiState
            get() = content.searchFieldState
        override val plugins: Map<PluginId, PluginRuntime>
            get() = content.plugins
        override val fullscreenContent: SearchFullscreenContentState
            get() = content
    }
}

sealed interface SearchScreenEvent {
    data class UpdateQuery(
        val query: String,
        val selection: SearchFieldSelection = SearchFieldSelection.CursorAtEnd
    ) : SearchScreenEvent

    data class OpenSearch(val query: String = "") : SearchScreenEvent
    data class OpenCommand(val query: String) : SearchScreenEvent
    data class Submit(val resultId: SearchResultId? = null) : SearchScreenEvent
    data class EnterAction(val action: FocusedCommandAction) : SearchScreenEvent
    data class EnterCallback(
        val resultId: SearchResultId,
        val callback: PluginCommandCallback,
        val updateUsage: Boolean = false
    ) : SearchScreenEvent
    data class EnterQuickAction(val resultId: SearchResultId) : SearchScreenEvent

    data class ShowContextActions(
        val resultId: SearchResultId,
        val sourceId: String,
        val actions: List<PluginCommandListAction>
    ) : SearchScreenEvent

    data class ShowResultContextActions(
        val resultId: SearchResultId,
        val sourceId: String
    ) : SearchScreenEvent

    data class UpdateAliasEditorInput(val value: String) : SearchScreenEvent
    data object SaveAliasEditor : SearchScreenEvent
    data object RemoveAlias : SearchScreenEvent
    data object DismissAliasEditor : SearchScreenEvent

    data class FocusPluginItem(val itemId: CommandItemId) : SearchScreenEvent
    data class EnterPluginItem(val itemId: CommandItemId) : SearchScreenEvent
    data object MoveFocusPrevious : SearchScreenEvent
    data object MoveFocusNext : SearchScreenEvent
    data object ToggleActions : SearchScreenEvent
    data object HideActions : SearchScreenEvent
    data object BackspaceOnEmpty : SearchScreenEvent
    data object CloseFullscreen : SearchScreenEvent
    data class DismissAlert(val alert: Alert) : SearchScreenEvent
    data class ConfirmAlert(val alert: Alert) : SearchScreenEvent
    data class DismissToast(val toastId: String) : SearchScreenEvent
}

private fun ShowToast.shouldBeReplacedBy(incoming: ShowToast): Boolean =
    toastId == incoming.toastId || matchesAnimatedLoadingToast(incoming)

private fun ShowToast.matchesAnimatedLoadingToast(incoming: ShowToast): Boolean =
    toast.style == NotificationEvent.Toast.Style.Animated &&
        incoming.toast.style == NotificationEvent.Toast.Style.Animated &&
        toast.message == incoming.toast.message

private fun SearchViewModelState.focusedResultId(): SearchResultId? =
    focusedItemIndex?.let { itemIndex ->
        searchResults?.results?.getOrNull(itemIndex)?.resultId
    }

private fun SearchViewModelState.overlayState(): SearchOverlayState =
    SearchOverlayState(
        focusedActions = focusedActions,
        contextActions = contextActions,
        showActions = showActions,
        showContextActions = showContextActions,
        activeContextSourceId = activeContextSourceId,
        aliasEditor = aliasEditor
    )

private fun SearchViewModelState.toScreenState(): SearchScreenState {
    val overlayState = overlayState()
    return when {
        fullscreen != null -> SearchScreenState.Fullscreen(
            content = fullscreen.copy(plugins = plugins),
            overlayState = overlayState,
            alerts = alerts,
            toasts = toasts
        )

        searchResults != null || !isSearching -> SearchScreenState.Results(
            content = SearchResultsContentState(
                searchFieldState = searchFieldState,
                searchResults = searchResults,
                plugins = plugins,
                focusedItemIndex = focusedItemIndex,
                isSearching = isSearching
            ),
            overlayState = overlayState,
            alerts = alerts,
            toasts = toasts
        )

        else -> SearchScreenState.Loading(
            content = SearchResultsContentState(
                searchFieldState = searchFieldState,
                searchResults = searchResults,
                plugins = plugins,
                focusedItemIndex = focusedItemIndex,
                isSearching = isSearching
            ),
            overlayState = overlayState,
            alerts = alerts,
            toasts = toasts
        )
    }
}

internal fun nextSearchResultsFocusIndex(currentIndex: Int?, resultCount: Int): Int? {
    if (resultCount <= 0) return null
    return currentIndex?.let { itemIndex ->
        (itemIndex + 1).coerceIn(0, resultCount - 1)
    } ?: 0
}

internal fun previousSearchResultsFocusIndex(currentIndex: Int?, resultCount: Int): Int? {
    if (resultCount <= 0) return null
    return currentIndex?.let { itemIndex ->
        (itemIndex - 1).coerceIn(0, resultCount - 1)
    } ?: (resultCount - 1)
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

internal suspend fun SearchResultSet.SearchResult.actions(
    plugins: Map<PluginId, PluginRuntime>
): List<SearchPanelAction> {
    val pluginActions = when (this) {
        is SearchResultSet.LiveSearchResult -> presentation.actions.toSearchPanelActions(updateUsage = false)
        is SearchResultSet.CachedSearchResult,
        is SearchResultSet.CommandSearchResult -> plugins[resultId.pluginId]?.actions(
            commandName = resultId.commandName,
            itemId = resultId.itemId
        ).orEmpty().toSearchPanelActions(updateUsage = true)
    }
    return pluginActions + hostAliasActions()
}

private fun SearchResultSet.SearchResult.hostAliasActions(): List<SearchPanelAction> {
    val alias = when (this) {
        is SearchResultSet.CachedSearchResult,
        is SearchResultSet.CommandSearchResult -> listEntry.alias
        is SearchResultSet.LiveSearchResult -> return emptyList()
    }
    val group = PluginUiText.Plain("Aliases")
    return if (alias == null) {
        listOf(
            SearchPanelAction(
                title = PluginUiText.Plain("Add Alias"),
                icon = PluginIcon.Builtin("Add"),
                group = group,
                kind = SearchPanelAction.Kind.OpenAliasEditor(existingAlias = null)
            )
        )
    } else {
        listOf(
            SearchPanelAction(
                title = PluginUiText.Plain("Edit Alias"),
                description = PluginUiText.Plain(alias),
                icon = PluginIcon.Builtin("Edit"),
                group = group,
                kind = SearchPanelAction.Kind.OpenAliasEditor(existingAlias = alias)
            ),
            SearchPanelAction(
                title = PluginUiText.Plain("Remove Alias"),
                description = PluginUiText.Plain(alias),
                icon = PluginIcon.Builtin("Delete"),
                group = group,
                destructive = true,
                kind = SearchPanelAction.Kind.RemoveAlias
            )
        )
    }
}

private fun SearchAliasInvalidReason.message(): String = when (this) {
    SearchAliasInvalidReason.Blank -> "Alias cannot be empty"
    SearchAliasInvalidReason.ContainsWhitespace -> "Alias cannot contain spaces"
    SearchAliasInvalidReason.NonAscii -> "Alias must use ASCII characters only"
}
