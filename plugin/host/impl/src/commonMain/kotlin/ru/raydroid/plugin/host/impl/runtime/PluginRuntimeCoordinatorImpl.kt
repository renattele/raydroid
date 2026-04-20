package ru.raydroid.plugin.host.impl.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.impl.ui.toPluginCommandPresentation
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListItem

internal class PluginRuntimeCoordinatorImpl(
    private val coroutineScope: CoroutineScope,
    private val pluginRuntimes: StateFlow<List<PluginRuntime>>
) : PluginRuntimeCoordinator {
    private val contentFlow = MutableStateFlow<List<PluginRuntimeCoordinator.ContentItem>>(
        emptyList()
    )

    private val cacheItemsFlow = MutableStateFlow<Map<PluginRuntime, List<SearchIndexMutation>>>(
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
                                val newContent = runtimes.flatMap { currentRuntime ->
                                    val content = currentRuntime.content().value
                                    content.map { contentItem ->
                                        PluginRuntimeCoordinator.ContentItem(
                                            listEntry = contentItem.presentation.listEntry.toPluginCommandListItem(
                                                currentRuntime.pluginId
                                            ),
                                            resultId = SearchResultId(
                                                pluginId = currentRuntime.pluginId,
                                                commandName = contentItem.commandName,
                                                itemId = contentItem.presentation.listEntry.id
                                            ),
                                            runtime = currentRuntime,
                                            presentation = contentItem.presentation.toPluginCommandPresentation(
                                                currentRuntime.pluginId
                                            ),
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

    override fun cachedItems(): Flow<Map<PluginRuntime, List<SearchIndexMutation>>> =
        cacheItemsFlow

    override fun runtimes(): StateFlow<List<PluginRuntime>> = pluginRuntimes

    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> = contentFlow

    override suspend fun update(
        query: String,
        action: CommandAction
    ) = supervisorScope {
        pluginRuntimes.value.map { runtime ->
            async {
                runCatching {
                    runtime.update(query, action)
                }
            }
        }.awaitAll()
        Unit
    }
}
