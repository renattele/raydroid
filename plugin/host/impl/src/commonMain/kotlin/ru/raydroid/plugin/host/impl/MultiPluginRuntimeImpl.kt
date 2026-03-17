package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.MultiPluginRuntime
import ru.raydroid.plugin.host.api.SinglePluginRuntime

internal class MultiPluginRuntimeImpl(
    coroutineScope: CoroutineScope,
    private val pluginRuntimes: StateFlow<List<SinglePluginRuntime>>
) : MultiPluginRuntime {
    private val contentFlow = MutableStateFlow<Map<SinglePluginRuntime, List<RayItems>>>(
        mapOf()
    )

    private val cacheItemsFlow = MutableStateFlow<Map<SinglePluginRuntime, List<ListItemUpdate>>>(
        mapOf()
    )

    init {
        coroutineScope.launch {
            var previousJob: Job? = null
            pluginRuntimes.collectLatest { runtimes ->
                previousJob?.cancel()
                previousJob = launch {
                    runtimes.forEach { runtime ->
                        launch {
                            runtime.content().collectLatest { _ ->
                                val newContent = runtimes.associateWith {
                                    it.content().value
                                }
                                contentFlow.value = newContent
                            }
                        }
                        launch {
                            runtime.cachedItems().collectLatest { newCacheItems ->
                                cacheItemsFlow.update { oldCacheItems ->
                                    oldCacheItems + (runtime to newCacheItems)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun cachedItems(): Flow<Map<SinglePluginRuntime, List<ListItemUpdate>>> =
        cacheItemsFlow

    override fun runtimes(): StateFlow<List<SinglePluginRuntime>> = pluginRuntimes

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