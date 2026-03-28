package ru.raydroid.plugin.host.impl

import app.cash.zipline.Zipline
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.SinglePluginRuntime

internal class SinglePluginRuntimeImpl(
    override val manifest: Manifest,
    override val resources: FileSystem,
    private val commandServices: List<CommandServiceBridge>,
    private val zipline: Zipline,
    private val pluginRuntimeDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope
) : SinglePluginRuntime {
    private val contentFlow =
        MutableStateFlow<List<RayItems>>(List(commandServices.size) { emptyMap() })

    private val invalidationChannel = MutableSharedFlow<InvalidationRequest>()
    override val pluginId: PluginId
        get() = PluginId(manifest.name)

    init {
        commandServices.forEachIndexed { index, command ->
            val renderRequest = object : CommandServiceBridge.RenderRequest {
                override fun requestRender() {
                    contentFlow.update { data ->
                        val updated = data.toMutableList()
                        updated[index] = command.content()
                        updated
                    }
                }
            }
            val invalidationRequest = object : CommandServiceBridge.InvalidateCacheRequest {
                override fun requestInvalidation(invalidatedIds: List<ItemId>?) {
                    coroutineScope.launch {
                        val serviceName = withContext(pluginRuntimeDispatcher) {
                            command.getServiceName()
                        }
                        invalidationChannel.emit(
                            InvalidationRequest(
                                serviceName,
                                invalidatedIds
                            )
                        )
                    }
                }
            }
            command.initialize(renderRequest, invalidationRequest)
        }
    }

    override fun cachedItems(
        chunkSize: Int
    ): Flow<List<ListItemUpdate>> = channelFlow {
        commandServices.forEach { command ->
            launch {
                val commandName = manifest.commands.find {
                    // Using plugin context because
                    // command.serviceName is actually a function
                    withContext(pluginRuntimeDispatcher) {
                        it.service == command.getServiceName()
                    }
                }?.service ?: return@launch
                withContext(pluginRuntimeDispatcher) {
                    command.cachedItems(chunkSize = chunkSize).collectLatest { chunk ->
                        send(chunk.map { it.toItemUpdate(commandName) })
                    }
                    withContext(coroutineScope.coroutineContext) {
                        invalidationChannel.collectLatest { invalidationRequest ->
                            if (invalidationRequest.commandName != commandName) return@collectLatest
                            if (invalidationRequest.invalidatedIds != null) {
                                val deleteUpdate =
                                    invalidationRequest.invalidatedIds.map { invalidatedId ->
                                        ListItemUpdate.Delete(
                                            listItemId = ListItemId(
                                                pluginId = PluginId(manifest.name),
                                                commandName = commandName,
                                                itemId = invalidatedId
                                            )
                                        )
                                    }
                                send(deleteUpdate)
                            } else {
                                send(
                                    listOf(
                                        ListItemUpdate.MarkAllAsOutdated(
                                            pluginId = PluginId(manifest.name),
                                            commandName = commandName
                                        )
                                    )
                                )
                            }
                            command.cachedItems(
                                invalidationRequest.invalidatedIds,
                                chunkSize = chunkSize
                            ).collect { chunk ->
                                send(chunk.map { it.toItemUpdate(commandName) })
                            }
                            if (invalidationRequest.invalidatedIds == null) {
                                send(
                                    listOf(
                                        ListItemUpdate.ClearOutdated(
                                            pluginId = PluginId(manifest.name),
                                            commandName = commandName
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ListItem.toItemUpdate(commandName: String) = ListItemUpdate.Upsert(
        listItemId = ListItemId(
            PluginId(manifest.name), commandName = commandName, itemId = id
        ), item = this
    )

    override fun content(): StateFlow<List<RayItems>> = contentFlow

    override suspend fun update(
        query: String, action: CommandAction
    ) {
        withContext(pluginRuntimeDispatcher) {
            commandServices.forEach { command ->
                command.update(query, action)
            }
        }
    }

    override suspend fun unload() {
        zipline.close()
    }

    private class InvalidationRequest(
        val commandName: String,
        val invalidatedIds: List<ItemId>?
    )
}
