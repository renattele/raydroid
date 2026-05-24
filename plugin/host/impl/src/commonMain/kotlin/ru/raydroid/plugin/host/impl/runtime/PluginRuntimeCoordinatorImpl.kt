package ru.raydroid.plugin.host.impl.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.impl.ui.toPluginIcon
import ru.raydroid.plugin.host.impl.ui.toPluginUiText

internal class PluginRuntimeCoordinatorImpl(
    private val coroutineScope: CoroutineScope,
    private val pluginRuntimes: StateFlow<List<PluginRuntime>>,
) : PluginRuntimeCoordinator {
    private val contentFlow =
        MutableStateFlow<List<PluginRuntimeCoordinator.ContentItem>>(
            emptyList(),
        )

    private val cacheItemsChannel = Channel<List<SearchIndexMutation>>(Channel.BUFFERED)
    private val commandFlow =
        MutableStateFlow<List<PluginRuntimeCoordinator.CommandItem>>(
            emptyList(),
        )

    init {
        coroutineScope.launch {
            var previousJob: Job? = null
            pluginRuntimes.collectLatest { runtimes ->
                previousJob?.cancel()
                val allContent = runtimes.allContentItems()
                contentFlow.value = allContent.visibleContentItems()
                commandFlow.value = runtimes.commandItems(allContent)
                previousJob =
                    launch {
                        runtimes.forEach { runtime ->
                            launch {
                                runtime.content().collectLatest { _ ->
                                    val allNewContent = runtimes.allContentItems()
                                    contentFlow.value = allNewContent.visibleContentItems()
                                    commandFlow.value = runtimes.commandItems(allNewContent)
                                }
                            }
                            launch {
                                runtime.cachedItems().collectLatest { newCacheItems ->
                                    cacheItemsChannel.send(newCacheItems)
                                }
                            }
                        }
                    }
            }
        }
    }

    override fun cachedItems(): Flow<List<SearchIndexMutation>> = cacheItemsChannel.receiveAsFlow()

    override fun runtimes(): StateFlow<List<PluginRuntime>> = pluginRuntimes

    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> = contentFlow

    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> = commandFlow

    override suspend fun update(action: CommandActionBridge) =
        supervisorScope {
            pluginRuntimes.value
                .map { runtime ->
                    async {
                        runCatching {
                            runtime.update(action)
                        }
                    }
                }.awaitAll()
            Unit
        }

    private fun List<PluginRuntime>.commandItems(
        content: List<PluginRuntimeCoordinator.ContentItem> = emptyList(),
    ): List<PluginRuntimeCoordinator.CommandItem> =
        flatMap { runtime ->
            runtime.manifest.commands
                .filter { command -> command.searchable }
                .map { command ->
                    val resultId =
                        SearchResultId(
                            pluginId = runtime.pluginId,
                            commandName = command.service,
                            itemId = CommandItemId.CommandRoot,
                        )
                    PluginRuntimeCoordinator.CommandItem(
                        runtime = runtime,
                        listEntry =
                            commandListEntry(
                                runtime = runtime,
                                command = command,
                                override = content.firstOrNull { item -> item.resultId == resultId }?.presentation,
                            ),
                        resultId = resultId,
                    )
                }
        }

    private fun List<PluginRuntime>.allContentItems(): List<PluginRuntimeCoordinator.ContentItem> =
        flatMap { currentRuntime ->
            currentRuntime.content().value.map { contentItem ->
                PluginRuntimeCoordinator.ContentItem(
                    listEntry = contentItem.presentation.listEntry,
                    resultId =
                        SearchResultId(
                            pluginId = currentRuntime.pluginId,
                            commandName = contentItem.commandName,
                            itemId = contentItem.presentation.listEntry.id,
                        ),
                    runtime = currentRuntime,
                    presentation = contentItem.presentation,
                )
            }
        }

    private fun List<PluginRuntimeCoordinator.ContentItem>.visibleContentItems(): List<PluginRuntimeCoordinator.ContentItem> =
        filterNot { contentItem ->
            contentItem.resultId.itemId == CommandItemId.CommandRoot
        }

    private fun commandListEntry(
        runtime: PluginRuntime,
        command: Command,
        override: PluginCommandPresentation?,
    ): PluginCommandListItem {
        val isResult = override?.listEntry?.trailingText != null
        return PluginCommandListItem(
            id = CommandItemId.CommandRoot,
            icon =
                if (isResult) {
                    override.listEntry.icon
                } else {
                    override?.listEntry?.icon ?: command.icon?.toPluginIcon(runtime.pluginId)
                },
            title = override?.listEntry?.title ?: command.title.toPluginUiText(runtime.pluginId),
            description =
                if (isResult) {
                    override.listEntry.description
                } else {
                    override?.listEntry?.description ?: command.description.toPluginUiText(runtime.pluginId)
                },
            enabled = override?.listEntry?.enabled ?: true,
            iconColor =
                override?.listEntry?.iconColor ?: command.icon
                    ?.takeIf { icon -> icon.type == Icon.Type.Builtin }
                    ?.let { PluginColor.OnSurfaceVariant },
            trailingText = override?.listEntry?.trailingText,
            alias = null,
        )
    }
}
