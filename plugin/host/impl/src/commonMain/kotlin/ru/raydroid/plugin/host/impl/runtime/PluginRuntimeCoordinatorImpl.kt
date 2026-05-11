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
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.impl.ui.toPluginIcon
import ru.raydroid.plugin.host.impl.ui.toPluginUiText

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
    private val commandFlow = MutableStateFlow<List<PluginRuntimeCoordinator.CommandItem>>(
        emptyList()
    )

    init {
        coroutineScope.launch {
            var previousJob: Job? = null
            pluginRuntimes.collectLatest { runtimes ->
                previousJob?.cancel()
                commandFlow.value = runtimes.commandItems()
                previousJob = launch {
                    runtimes.forEach { runtime ->
                        launch {
                            runtime.content().collectLatest { _ ->
                                val newContent = runtimes.flatMap { currentRuntime ->
                                    val content = currentRuntime.content().value
                                    content.map { contentItem ->
                                        PluginRuntimeCoordinator.ContentItem(
                                            listEntry = contentItem.presentation.listEntry,
                                            resultId = SearchResultId(
                                                pluginId = currentRuntime.pluginId,
                                                commandName = contentItem.commandName,
                                                itemId = contentItem.presentation.listEntry.id
                                            ),
                                            runtime = currentRuntime,
                                            presentation = contentItem.presentation,
                                        )
                                    }
                                }
                                contentFlow.value = newContent.filterNot { contentItem ->
                                    contentItem.resultId.itemId == CommandItemId.CommandRoot
                                }
                                commandFlow.value = runtimes.commandItems(newContent)
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

    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> = commandFlow

    override suspend fun update(
        action: CommandActionBridge
    ) = supervisorScope {
        pluginRuntimes.value.map { runtime ->
            async {
                runCatching {
                    runtime.update(action)
                }
            }
        }.awaitAll()
        Unit
    }

    private fun List<PluginRuntime>.commandItems(
        content: List<PluginRuntimeCoordinator.ContentItem> = emptyList()
    ): List<PluginRuntimeCoordinator.CommandItem> =
        flatMap { runtime ->
            runtime.manifest.commands
                .filter { command -> command.searchable }
                .map { command ->
                    val resultId = SearchResultId(
                        pluginId = runtime.pluginId,
                        commandName = command.service,
                        itemId = CommandItemId.CommandRoot
                    )
                    PluginRuntimeCoordinator.CommandItem(
                        runtime = runtime,
                        listEntry = commandListEntry(
                            runtime = runtime,
                            command = command,
                            override = content.firstOrNull { item -> item.resultId == resultId }?.presentation
                        ),
                        resultId = resultId
                    )
                }
        }

    private fun commandListEntry(
        runtime: PluginRuntime,
        command: Command,
        override: PluginCommandPresentation?
    ): PluginCommandListItem =
        PluginCommandListItem(
            id = CommandItemId.CommandRoot,
            icon = override?.listEntry?.icon ?: command.icon?.toPluginIcon(runtime.pluginId),
            title = override?.listEntry?.title ?: command.title.toPluginUiText(runtime.pluginId),
            description = override?.listEntry?.description ?: command.description.toPluginUiText(runtime.pluginId),
            enabled = override?.listEntry?.enabled ?: true
        )
}
