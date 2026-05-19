package ru.raydroid.plugin.host.impl.runtime

import app.cash.zipline.Zipline
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.api.runtime.CommandServiceBridge
import ru.raydroid.plugin.api.runtime.InternalCommandActionBridge
import ru.raydroid.plugin.api.ui.FormValues
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListAction
import ru.raydroid.plugin.host.impl.ui.toPluginCommandPresentation
import ru.raydroid.plugin.host.impl.ui.toPluginRayNodeData

internal class PluginRuntimeImpl(
    override val manifest: Manifest,
    override val resources: FileSystem,
    private val commandServices: List<CommandServiceBridge>,
    private val zipline: Zipline,
    private val pluginRuntimeDispatcher: CoroutineDispatcher,
    private val coroutineScope: CoroutineScope
) : PluginRuntime {
    private val contentFlow =
        MutableStateFlow<List<PluginRuntime.ContentItem>>(emptyList())
    private val fullscreenFlows =
        mutableMapOf<String, MutableStateFlow<PluginRuntime.FullscreenContent?>>()

    private val invalidationChannel = MutableSharedFlow<InvalidationRequest>()
    override val pluginId: PluginId
        get() = PluginId(manifest.name)

    init {
        coroutineScope.launch {
            val commandNames = withContext(pluginRuntimeDispatcher) {
                commandServices.associateWith {
                    it.getServiceName()
                }
            }
            commandServices.forEach { command ->
                val commandName = commandNames[command] ?: "Unknown"
                val renderRequest = object : CommandServiceBridge.RenderRequest {
                    override fun requestRender() {
                        coroutineScope.launch(pluginRuntimeDispatcher) {
                            setCommandContent(commandName, command)
                        }
                    }
                }
                val fullscreenRenderRequest = object : CommandServiceBridge.FullscreenRenderRequest {
                    override fun requestFullscreenRender() {
                        coroutineScope.launch(pluginRuntimeDispatcher) {
                            setFullscreenContent(commandName, command)
                        }
                    }
                }
                val invalidationRequest = object : CommandServiceBridge.InvalidateCacheRequest {
                    override fun requestInvalidation(invalidatedIds: List<CommandItemId>?) {
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
                withContext(pluginRuntimeDispatcher) {
                    command.initialize(renderRequest, fullscreenRenderRequest, invalidationRequest)
                }
            }
        }
    }

    override fun cachedItems(
        chunkSize: Int
    ): Flow<List<SearchIndexMutation>> = channelFlow {
        commandServices.forEach { command ->
            launch {
                val commandName = manifest.commands.find {
                    // Using plugin context because
                    // command.serviceName is actually a function
                    withContext(pluginRuntimeDispatcher) {
                        it.service == command.getServiceName()
                    }
                }?.service ?: return@launch
                send(
                    listOf(
                        SearchIndexMutation.MarkAllAsOutdated(
                            pluginId = PluginId(manifest.name),
                            commandName = commandName
                        )
                    )
                )
                withContext(pluginRuntimeDispatcher) {
                    command.cachedItems(chunkSize = chunkSize).collectLatest { chunk ->
                        send(chunk.map { it.toMutation(commandName) })
                    }
                }
                send(
                    listOf(
                        SearchIndexMutation.ClearOutdated(
                            pluginId = PluginId(manifest.name),
                            commandName = commandName
                        )
                    )
                )
                invalidationChannel.collectLatest { invalidationRequest ->
                    if (invalidationRequest.commandName != commandName) return@collectLatest
                    if (invalidationRequest.invalidatedIds != null) {
                        val deleteUpdate =
                            invalidationRequest.invalidatedIds.map { invalidatedId ->
                                SearchIndexMutation.Delete(
                                    resultId = SearchResultId(
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
                                SearchIndexMutation.MarkAllAsOutdated(
                                    pluginId = PluginId(manifest.name),
                                    commandName = commandName
                                )
                            )
                        )
                    }
                    withContext(pluginRuntimeDispatcher) {
                        command.cachedItems(
                            invalidationRequest.invalidatedIds,
                            chunkSize = chunkSize
                        ).collect { chunk ->
                            send(chunk.map { it.toMutation(commandName) })
                        }
                    }
                    if (invalidationRequest.invalidatedIds == null) {
                        send(
                            listOf(
                                SearchIndexMutation.ClearOutdated(
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

    override suspend fun cachedItems(
        commandName: String,
        requestedItems: List<CommandItemId>,
        chunkSize: Int
    ): List<SearchIndexMutation> {
        if (requestedItems.isEmpty()) return emptyList()
        return withContext(pluginRuntimeDispatcher) {
            val command = commandServices.firstOrNull { service ->
                service.getServiceName() == commandName
            } ?: return@withContext emptyList()
            buildList {
                command.cachedItems(
                    requestedItems = requestedItems,
                    chunkSize = chunkSize
                ).collect { chunk ->
                    addAll(chunk.map { item -> item.toMutation(commandName) })
                }
            }
        }
    }

    private fun CommandListItem.toMutation(commandName: String) = SearchIndexMutation.Upsert(
        resultId = SearchResultId(
            PluginId(manifest.name), commandName = commandName, itemId = id
        ), listEntry = this
    )

    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> = contentFlow

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> =
        fullscreenFlow(commandName)

    override suspend fun actions(
        commandName: String,
        itemId: CommandItemId
    ): List<PluginCommandListAction> {
        return withContext(pluginRuntimeDispatcher) {
            val command = commandServices.firstOrNull { command ->
                command.getServiceName() == commandName
            } ?: return@withContext emptyList()
            command.actions(CommandActionTarget(itemId)).map { action ->
                action.toPluginCommandListAction(pluginId, dispatchCallback(command))
            }
        }
    }

    override suspend fun update(
        action: CommandActionBridge
    ) {
        withContext(pluginRuntimeDispatcher) {
            commandServices.forEach { command ->
                launch {
                    command.update(action)
                }
            }
        }
    }

    override suspend fun update(
        commandName: String,
        action: CommandActionBridge
    ) {
        withContext(pluginRuntimeDispatcher) {
            val command = commandServices.firstOrNull { command ->
                command.getServiceName() == commandName
            } ?: return@withContext
            command.update(action)
            if (action is CommandActionBridge.Regular) {
                when (action.action) {
                    is CommandAction.OpenCommand -> setFullscreenContent(commandName, command)
                    is CommandAction.CloseCommand -> fullscreenFlow(commandName).value = null
                    else -> Unit
                }
            }
        }
    }

    override suspend fun back(commandName: String): Boolean {
        return withContext(pluginRuntimeDispatcher) {
            val command = commandServices.firstOrNull { command ->
                command.getServiceName() == commandName
            } ?: return@withContext false
            command.back()
        }
    }

    override suspend fun unload() {
        withContext(pluginRuntimeDispatcher) {
            zipline.close()
        }
    }

    private fun setCommandContent(
        commandName: String,
        command: CommandServiceBridge
    ) {
        val dispatchCallback = dispatchCallback(command)
        val content = command.content().values.map {
            PluginRuntime.ContentItem(
                commandName = commandName,
                presentation = it.toPluginCommandPresentation(pluginId, dispatchCallback, dispatchFormCallback(command))
            )
        }
        contentFlow.update { data ->
            data.filter { contentItem ->
                contentItem.commandName != commandName
            } + content
        }
    }

    private fun fullscreenFlow(commandName: String): MutableStateFlow<PluginRuntime.FullscreenContent?> {
        return fullscreenFlows.getOrPut(commandName) {
            MutableStateFlow(null)
        }
    }

    private fun setFullscreenContent(
        commandName: String,
        command: CommandServiceBridge
    ) {
        fullscreenFlow(commandName).value = PluginRuntime.FullscreenContent(
            commandName = commandName,
            content = command.fullscreen().map { node ->
                node.toPluginRayNodeData(pluginId, dispatchCallback(command), dispatchFormCallback(command))
            }
        )
    }

    private fun dispatchCallback(
        command: CommandServiceBridge
    ): suspend (CommandCallbackRef) -> Unit = { callback ->
        withContext(pluginRuntimeDispatcher) {
            command.update(
                CommandActionBridge.Internal(
                    InternalCommandActionBridge.Click(callback)
                )
            )
        }
    }

    private fun dispatchFormCallback(
        command: CommandServiceBridge
    ): suspend (CommandCallbackRef, FormValues) -> Unit = { callback, values ->
        withContext(pluginRuntimeDispatcher) {
            command.update(
                CommandActionBridge.Internal(
                    InternalCommandActionBridge.SubmitForm(callback, values)
                )
            )
        }
    }

    private class InvalidationRequest(
        val commandName: String,
        val invalidatedIds: List<CommandItemId>?
    )
}
