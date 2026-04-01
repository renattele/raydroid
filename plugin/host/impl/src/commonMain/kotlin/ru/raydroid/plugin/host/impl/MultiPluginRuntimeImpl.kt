package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.MultiPluginRuntime
import ru.raydroid.plugin.host.api.SinglePluginRuntime

internal class MultiPluginRuntimeImpl(
    private val coroutineScope: CoroutineScope,
    private val pluginRuntimes: StateFlow<List<SinglePluginRuntime>>
) : MultiPluginRuntime {
    private val contentFlow = MutableStateFlow<List<MultiPluginRuntime.ContentItem>>(
        emptyList()
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
                                val newContent = runtimes.flatMap {
                                    val content = it.content().value
                                    content.map { contentItem ->
                                        MultiPluginRuntime.ContentItem(
                                            listItem = contentItem.item.listItem,
                                            listItemId = ListItemId(
                                                pluginId = runtime.pluginId,
                                                commandName = contentItem.commandName,
                                                itemId = contentItem.item.listItem.id
                                            ),
                                            runtime = runtime,
                                            item = contentItem.item,
                                        )
                                    }
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

    override fun content(): StateFlow<List<MultiPluginRuntime.ContentItem>> = contentFlow

    override suspend fun update(
        query: String,
        action: CommandAction
    ) {
        pluginRuntimes.value.map { runtime ->
            coroutineScope.async {
                runtime.update(query, action)
            }
        }.awaitAll()
    }
}