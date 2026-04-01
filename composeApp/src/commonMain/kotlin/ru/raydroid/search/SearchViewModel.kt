package ru.raydroid.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.NotificationEvent
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.SearchResults
import ru.raydroid.plugin.host.api.SinglePluginRuntime
import ru.raydroid.plugin.host.api.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.usecase.OpenItemUseCase
import ru.raydroid.plugin.host.api.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.impl.ui.SearchFieldState

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val syncCacheUseCase: SyncCacheUseCase,
    private val loadRuntimesUseCase: LoadRuntimesUseCase,
    private val searchUseCase: SearchUseCase,
    private val getPluginsUseCase: GetPluginsUseCase,
    private val openItemUseCase: OpenItemUseCase,
    private val getEventsUseCase: GetEventsUseCase,
    private val emitEventUseCase: EmitEventUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchScreenState(
            eventSink = ::onEvent
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            launch { syncCacheUseCase() }
            launch { loadRuntimesUseCase() }
            launch {
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
            launch {
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
            snapshotFlow { _state.value.searchFieldState.fieldState.text }
                .distinctUntilChanged()
                .collectLatest { query ->
                    var updatedFocus = false
                    searchUseCase(query.toString()).collectLatest { searchResults ->
                        println(searchResults)
                        _state.update { state ->
                            state.copy(
                                searchResults = searchResults,
                                focusedItemIndex = if (!updatedFocus) {
                                    if (searchResults.results.isNotEmpty()) {
                                        0
                                    } else {
                                        null
                                    }
                                } else {
                                    state.focusedItemIndex
                                }
                            )
                        }
                        updatedFocus = true
                    }
                }
        }
    }

    private fun onEvent(event: SearchScreenEvent) {
        println("Handling: $event")
        println("Before state: ${_state.value}")
        viewModelScope.launch {
            when (event) {
                is SearchScreenEvent.Enter -> {
                    val state = _state.value
                    val openListItemId =
                        event.listItemId ?: state.focusedItemIndex?.let { focusedItemIndex ->
                            state.searchResults?.results[focusedItemIndex]?.listItemId
                        }
                    if (openListItemId == null) {
                        return@launch
                    }
                    openItemUseCase(
                        state.searchFieldState.fieldState.text.toString(),
                        openListItemId
                    )
                }

                SearchScreenEvent.MoveFocusNext -> {
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = state.searchResults?.let { searchResults ->
                                state.focusedItemIndex?.let { itemIndex ->
                                    (itemIndex + 1).coerceIn(0, searchResults.results.size)
                                }
                            }
                        )
                    }
                }

                SearchScreenEvent.MoveFocusPrevious -> {
                    _state.update { state ->
                        state.copy(
                            focusedItemIndex = state.searchResults?.let { searchResults ->
                                state.focusedItemIndex?.let { itemIndex ->
                                    (itemIndex - 1).coerceIn(0, searchResults.results.size)
                                }
                            }
                        )
                    }
                }

                is SearchScreenEvent.QueryChanged -> {

                }

                is SearchScreenEvent.DismissAlert -> {
                    _state.update { state ->
                        state.copy(
                            alerts = state.alerts - event.alert
                        )
                    }
                    emitEventUseCase.invoke(
                        event.alert.pluginId,
                        NotificationEvent.AlertResult(event.alert.dismissAction)
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
                        NotificationEvent.AlertResult(event.alert.confirmAction)
                    )
                }

                is SearchScreenEvent.HideToast -> {
                    _state.update { state ->
                        state.copy(
                            toasts = state.toasts - event.toast
                        )
                    }
                }
            }
            println("After state: ${_state.value}")
        }
    }
}

data class SearchScreenState(
    val searchFieldState: SearchFieldState = SearchFieldState(
        fieldState = TextFieldState()
    ),
    val searchResults: SearchResults? = null,
    val plugins: Map<PluginId, SinglePluginRuntime> = emptyMap(),
    val focusedItem: ListItem? = null,
    val focusedItemIndex: Int? = null,
    val alerts: List<NotificationEvent.Alert> = emptyList(),
    val toasts: List<NotificationEvent.ShowToast> = emptyList(),
    val eventSink: (SearchScreenEvent) -> Unit
)

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
    data class Enter(val listItemId: ListItemId? = null) : SearchScreenEvent
    data object MoveFocusPrevious : SearchScreenEvent
    data object MoveFocusNext : SearchScreenEvent
    data class DismissAlert(val alert: NotificationEvent.Alert) : SearchScreenEvent
    data class ConfirmAlert(val alert: NotificationEvent.Alert) : SearchScreenEvent
    data class HideToast(val toast: NotificationEvent.ShowToast) : SearchScreenEvent
}
