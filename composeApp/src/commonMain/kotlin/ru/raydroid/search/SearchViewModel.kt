package ru.raydroid.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.application.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetCommandFullscreenUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenItemUseCase
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.event.NotificationEvent.*
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.api.ui.toPluginUiText
import ru.raydroid.plugin.host.impl.presentation.SearchFieldState

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val syncCacheUseCase: SyncCacheUseCase,
    private val loadRuntimesUseCase: LoadRuntimesUseCase,
    private val searchUseCase: SearchUseCase,
    private val getPluginsUseCase: GetPluginsUseCase,
    private val openItemUseCase: OpenItemUseCase,
    private val getCommandFullscreenUseCase: GetCommandFullscreenUseCase,
    private val getEventsUseCase: GetEventsUseCase,
    private val emitEventUseCase: EmitEventUseCase
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
                    if (it.isNotEmpty()) {
                        _state.update { state ->
                            val fullscreen = state.fullscreen ?: return@update state
                            state.copy(
                                fullscreen = fullscreen.copy(exitBackspaceCount = 0)
                            )
                        }
                    }
                    _state.update { state ->
                        state.copy(
                            isSearching = state.searchResults == null,
                            focusedItemIndex = state.searchResults
                                ?.results
                                ?.takeIf { results -> results.isNotEmpty() }
                                ?.let { 0 },
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
                                showActions = false
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
                    openItemUseCase(
                        state.searchFieldState.fieldState.text.toString(),
                        openResultId
                    )
                    if (openResult is SearchResultSet.CommandSearchResult) {
                        collectFullscreen(openResult)
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
                        delay(FULLSCREEN_COLLAPSE_DELAY_MS)
                        closeFullscreen(_state.value)
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
                    val state = _state.value
                    val openResultId = state.focusedResultId() ?: return@launch
                    openItemUseCase(
                        state.searchFieldState.fieldState.text.toString(),
                        openResultId,
                        event.action.id
                    )
                }

                SearchScreenEvent.CloseFullscreen -> {
                    val state = _state.value
                    closeFullscreen(state)
                }
            }
        }
    }

    private suspend fun closeFullscreen(state: SearchScreenState) {
        val fullscreen = state.fullscreen ?: return
        fullscreenJob?.cancel()
        fullscreenJob = null
        openItemUseCase.closeCommand(
            query = state.searchFieldState.fieldState.text.toString(),
            resultId = fullscreen.resultId
        )
        _state.update { currentState ->
            currentState.copy(fullscreen = null)
        }
        _state.value.searchFieldState.fieldState.setTextAndSelectAll(fullscreen.previousQuery)
    }

    private fun collectFullscreen(result: SearchResultSet.CommandSearchResult) {
        fullscreenJob?.cancel()
        val previousQuery = _state.value.searchFieldState.fieldState.text.toString()
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
                    previousQuery = previousQuery,
                    exitBackspaceCount = 0,
                    content = emptyList()
                ),
                showActions = false
            )
        }
        _state.value.searchFieldState.fieldState.setTextAndPlaceCursorAtEnd("")
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
    val searchFieldState: SearchFieldState = SearchFieldState(
        fieldState = TextFieldState()
    ),
    val searchResults: SearchResultSet? = null,
    val plugins: Map<PluginId, PluginRuntime> = emptyMap(),
    val focusedItem: PluginCommandListItem? = null,
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
    val previousQuery: String,
    val exitBackspaceCount: Int,
    val content: List<PluginRayNodeData>
)

private fun SearchScreenState.focusedResultId(): SearchResultId? =
    focusedItemIndex?.let { itemIndex ->
        searchResults?.results?.getOrNull(itemIndex)?.resultId
    }

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
    data class Enter(val resultId: SearchResultId? = null) : SearchScreenEvent
    data class EnterAction(val action: PluginCommandListAction) : SearchScreenEvent
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
