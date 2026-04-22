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
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.application.usecase.CloseCommandUseCase
import ru.raydroid.plugin.host.api.application.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.application.usecase.EnterItemUseCase
import ru.raydroid.plugin.host.api.application.usecase.ExecuteCommandActionUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetCommandActionsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetCommandFullscreenUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetSearchFieldRequestsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenLiveEntryUseCase
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
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
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
    private val executeCommandActionUseCase: ExecuteCommandActionUseCase,
    private val openLiveEntryUseCase: OpenLiveEntryUseCase,
    private val getCommandActionsUseCase: GetCommandActionsUseCase,
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncCacheUseCase()
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadRuntimesUseCase()
        }
        viewModelScope.launch {
            getPluginsUseCase().collectLatest { plugins ->
                _state.update { state ->
                    state.copy(
                        plugins = plugins.associateBy {
                            it.pluginId
                        }
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
                        _state.update { state ->
                            state.copy(
                                toasts = state.toasts + data
                            )
                        }
                    }

                    is NotificationEvent.HideToast -> {
                        _state.update { state ->
                            state.copy(
                                toasts = state.toasts - NotificationEvent.ShowToast(
                                    pluginId = data.pluginId,
                                    toast = data.toast
                                )
                            )
                        }
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
                            showActions = false
                        )
                    }
                }
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { query ->
                    var focusInitialized = false
                    searchUseCase(query).collectLatest { searchResults ->
                        _state.update { state ->
                            val focusedIndex = if (!focusInitialized) {
                                searchResults.results.takeIf { it.isNotEmpty() }?.let { 0 }
                            } else {
                                searchResults.results.takeIf { it.isNotEmpty() }?.let { results ->
                                    state.focusedItemIndex?.coerceIn(0, results.lastIndex)
                                }
                            }
                            focusInitialized = true
                            state.copy(
                                searchResults = searchResults,
                                focusedItemIndex = focusedIndex,
                                isSearching = false,
                                focusedActions = if (focusedIndex == state.focusedItemIndex) {
                                    state.focusedActions
                                } else {
                                    emptyList()
                                },
                                showActions = false
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
        viewModelScope.launch {
            _state
                .map { state -> state.focusedActionRequest() }
                .distinctUntilChanged()
                .collectLatest { request ->
                    if (request == null) {
                        _state.update { state ->
                            state.copy(
                                focusedActions = emptyList(),
                                showActions = false
                            )
                        }
                        return@collectLatest
                    }
                    val actions = getCommandActionsUseCase(
                        resultId = request.resultId,
                        target = request.target
                    ).map { action ->
                        FocusedCommandAction(
                            resultId = request.resultId,
                            action = action
                        )
                    }
                    _state.update { state ->
                        if (state.focusedActionRequest() != request) {
                            state
                        } else {
                            state.copy(
                                focusedActions = actions,
                                showActions = state.showActions && actions.isNotEmpty()
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
                            openLiveEntryUseCase(openResultId)
                        }
                        else -> {
                            enterItemUseCase(openResultId)
                        }
                    }
                }

                SearchScreenEvent.MoveFocusNext -> {
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = state.searchResults?.let { searchResults ->
                                state.focusedItemIndex?.let { itemIndex ->
                                    (itemIndex + 1).coerceIn(0, searchResults.results.lastIndex)
                                }
                            },
                            focusedActions = emptyList(),
                            showActions = false
                        )
                    }
                }

                SearchScreenEvent.MoveFocusPrevious -> {
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = state.searchResults?.let { searchResults ->
                                state.focusedItemIndex?.let { itemIndex ->
                                    (itemIndex - 1).coerceIn(0, searchResults.results.lastIndex)
                                }
                            },
                            focusedActions = emptyList(),
                            showActions = false
                        )
                    }
                }

                SearchScreenEvent.ToggleActions -> {
                    _state.update { state ->
                        state.copy(
                            showActions = !state.showActions
                        )
                    }
                }

                SearchScreenEvent.HideActions -> {
                    _state.update { state ->
                        state.copy(
                            showActions = false
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
                    _state.update { state ->
                        state.copy(
                            toasts = state.toasts - event.toast
                        )
                    }
                }

                is SearchScreenEvent.EnterAction -> {
                    executeCommandActionUseCase(
                        event.action.resultId,
                        event.action.action.id
                    )
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
                    content = emptyList()
                ),
                focusedActions = emptyList(),
                showActions = false
            )
        }
        fullscreenJob = viewModelScope.launch {
            getCommandFullscreenUseCase(result.resultId).collectLatest { content ->
                _state.update { state ->
                    val fullscreen = state.fullscreen
                    if (fullscreen?.resultId != result.resultId) {
                        state
                    } else {
                        state.copy(
                            fullscreen = fullscreen.copy(
                                content = content?.content.orEmpty()
                            )
                        )
                    }
                }
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
    val focusedItemIndex: Int? = null,
    val isSearching: Boolean = false,
    val showActions: Boolean = false,
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
    val content: List<PluginRayNodeData>
)

data class FocusedCommandAction(
    val resultId: SearchResultId,
    val action: PluginCommandListAction
)

private data class CommandActionRequest(
    val resultId: SearchResultId,
    val target: CommandActionTarget,
    val query: String,
    val contentKey: Int
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

private fun SearchScreenState.focusedActionRequest(): CommandActionRequest? {
    val fullscreen = fullscreen
    if (fullscreen != null) {
        return CommandActionRequest(
            resultId = fullscreen.resultId,
            target = CommandActionTarget.Fullscreen,
            query = fullscreen.searchFieldState.fieldState.text.toString(),
            contentKey = fullscreen.content.hashCode()
        )
    }
    val resultId = focusedResultId() ?: return null
    return CommandActionRequest(
        resultId = resultId,
        target = if (resultId.itemId == CommandItemId.CommandRoot) {
            CommandActionTarget.CommandRoot
        } else {
            CommandActionTarget.Item(resultId.itemId)
        },
        query = searchFieldState.fieldState.text.toString(),
        contentKey = searchResults.hashCode()
    )
}

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
    data class Enter(val resultId: SearchResultId? = null) : SearchScreenEvent
    data class EnterAction(val action: FocusedCommandAction) : SearchScreenEvent
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
