package ru.raydroid.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.host.api.PluginRepository

class SearchViewModel(private val pluginRepository: PluginRepository) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchScreenState(
            eventSink = ::onEvent
        )
    )
    val state = _state.asStateFlow()

    private fun onEvent(event: SearchScreenEvent) {
        when (event) {
            is SearchScreenEvent.QueryChanged -> {
                _state.update { state ->
                    state.copy(query = event.query)
                }
            }
        }
    }
}

data class SearchScreenState(
    val query: String = "",
    val eventSink: (SearchScreenEvent) -> Unit
)

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
}