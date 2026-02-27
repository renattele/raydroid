package ru.raydroid.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.api.PluginRepository
import ru.raydroid.plugin.host.api.SinglePluginRuntime

class SearchViewModel(
    private val pluginRepository: PluginRepository,
    private val pluginLoader: PluginLoader
) : ViewModel() {
    private val _state = MutableStateFlow(
        SearchScreenState(
            eventSink = ::onEvent
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val pluginIds = pluginRepository.listInstalledPlugins()
            val runtimes = pluginIds.associateWith { pluginId ->
                val rawPlugin = pluginRepository.loadPlugin(pluginId) ?: return@associateWith null
                pluginLoader.loadPlugin(rawPlugin)
            }.filter { (_, runtime) -> runtime != null } as Map<PluginId, SinglePluginRuntime>
            val multiRuntime = pluginLoader.join(MutableStateFlow(runtimes.values.toList()))
            launch {
                multiRuntime.content().collect { content ->
                    val newContent = content.map { (runtime, items) ->
                        SearchScreenState.PluginResultContent(
                            resources = runtime.resources,
                            manifest = runtime.manifest,
                            data = items.flatMap { it.values }.flatten()
                        )
                    }
                    _state.update {
                        it.copy(content = newContent)
                    }
                }
            }
            var lastChangedQuery = state.value.query
            state.collect { state ->
                // Check needed to prevent infinite loops like this:
                // query update -> content update -> query update -> content update
                if (state.query != lastChangedQuery) {
                    println("UPDATING")
                    multiRuntime.update(query = state.query, action = CommandAction.Type())
                    lastChangedQuery = state.query
                }
            }
        }
    }

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