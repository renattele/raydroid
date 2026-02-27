package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.api.MultiPluginRuntime
import ru.raydroid.plugin.host.api.SinglePluginRuntime

class MultiPluginRuntimeImpl(
    coroutineScope: CoroutineScope,
    private val pluginRuntimes: StateFlow<List<SinglePluginRuntime>>
) : MultiPluginRuntime {
    private val contentFlow = MutableStateFlow<Map<SinglePluginRuntime, List<RayItems>>>(
        mapOf()
    )

    init {
        coroutineScope.launch {
            var previousJob: Job? = null
            pluginRuntimes.collect { runtimes ->
                previousJob?.cancel()
                previousJob = launch {
                    runtimes.forEach { runtime ->
                        launch {
                            runtime.content().collect { _ ->
                                val newContent = runtimes.associateWith {
                                    it.content().value
                                }
                                contentFlow.value = newContent
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun cachedItems(): List<ListItem> {
        return pluginRuntimes.value.flatMap { it.cachedItems() }
    }

    override fun content(): StateFlow<Map<SinglePluginRuntime, List<RayItems>>> = contentFlow

    override suspend fun update(
        query: String,
        action: CommandAction
    ) {
        pluginRuntimes.value.forEach { runtime ->
            runtime.update(query, action)
        }
    }
}