package ru.raydroid.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.host.api.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.impl.ui.SearchFieldState

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val syncCacheUseCase: SyncCacheUseCase,
    private val loadRuntimesUseCase: LoadRuntimesUseCase,
    private val searchUseCase: SearchUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchScreenState(
            eventSink = ::onEvent
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO  + SupervisorJob()) {
            launch { syncCacheUseCase() }
            launch { loadRuntimesUseCase() }
            _state
                .map { it.searchFieldState.fieldState.text.toString() }
                .distinctUntilChanged()
                .collectLatest { query ->
                searchUseCase(query).collect { searchResults ->
                    println(searchResults)
                }
            }
        }
    }

    private fun onEvent(event: SearchScreenEvent) {
        when (event) {
            is SearchScreenEvent.QueryChanged -> {
            }
        }
    }
}

data class SearchScreenState(
    val searchFieldState: SearchFieldState = SearchFieldState(
        fieldState = TextFieldState()
    ),
    val content: List<PluginResultContent> = emptyList(),
    val eventSink: (SearchScreenEvent) -> Unit
) {
    data class PluginResultContent(
        val resources: FileSystem,
        val manifest: Manifest,
        val data: List<RayNodeData>
    )
}

@Immutable
sealed interface SearchScreenEvent {
    data class QueryChanged(val query: String) : SearchScreenEvent
}
