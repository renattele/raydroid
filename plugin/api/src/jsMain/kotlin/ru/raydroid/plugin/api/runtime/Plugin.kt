package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.presentation.CommandPresentationMap
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.buildRayNodes
import ru.raydroid.plugin.api.ui.toRayModifier
import ru.raydroid.plugin.api.zipline

@DslMarker
annotation class PluginMarker

interface PluginScope {
    @PluginMarker
    fun command(commandService: CommandService)
}

@PluginMarker
fun plugin(content: PluginScope.() -> Unit) {
    val scope = object : PluginScope {
        override fun command(commandService: CommandService) {
            val serviceName = commandService::class.simpleName!!
            zipline.bind(serviceName, commandService.toBridge(serviceName))
        }
    }
    scope.content()
}

internal fun CommandService.toBridge(serviceName: String): CommandServiceBridge =
    object : CommandServiceBridge {
        private val entryIdPrefix = "__ray_entry:"

        override fun getServiceName() = serviceName

        override suspend fun cachedItems(
            requestedItems: List<CommandItemId>?,
            chunkSize: Int
        ): Flow<List<CommandListItem>> {
            return this@toBridge.cachedItems(requestedItems, chunkSize)
        }

        override fun content(): CommandPresentationMap {
            val presentations = mutableMapOf<CommandItemId, CommandPresentation>()
            val frame = contentCallbacks.beginFrame()
            var entryIndex = 0
            val scope = object : CommandListScope {
                override fun entry(
                    title: UiText?,
                    description: UiText?,
                    icon: Icon?,
                    modifier: Modifier,
                    content: RayScope.() -> Unit
                ) {
                    val entryPath = "$entryIdPrefix${entryIndex++}"
                    val entryModifier = modifier.toRayModifier(entryPath, frame::register)
                    val actions = entryModifier?.actions.orEmpty()
                    val primaryCallback = actions.firstOrNull { it.primary }?.callback
                        ?: actions.firstOrNull()?.callback
                    val id = CommandItemId(primaryCallback?.id?.value ?: entryPath)
                    val listEntry = CommandListItem(
                        id = id,
                        title = title,
                        description = description,
                        icon = icon,
                        enabled = entryModifier?.enabled ?: true,
                    )
                    presentations[id] = CommandPresentation(
                        listEntry = listEntry,
                        primaryCallback = primaryCallback,
                        actions = actions,
                        content = buildRayNodes(
                            registerCallback = frame::register,
                            content = content
                        )
                    )
                }
            }
            scope.content()
            return presentations
        }

        override fun fullscreen(): List<RayNodeData> {
            val frame = fullscreenCallbacks.beginFrame()
            return buildRayNodes(registerCallback = frame::register, content = fullscreenContent@{
                with(this@toBridge) {
                    this@fullscreenContent.fullscreen()
                }
            })
        }

        override fun initialize(
            request: CommandServiceBridge.RenderRequest,
            fullscreenRenderRequest: CommandServiceBridge.FullscreenRenderRequest,
            invalidateCacheRequest: CommandServiceBridge.InvalidateCacheRequest
        ) {
            onRenderRequest = request::requestRender
            onFullscreenRenderRequest = fullscreenRenderRequest::requestFullscreenRender
            onInvalidateCacheRequest = invalidateCacheRequest::requestInvalidation
        }

        override suspend fun update(action: CommandActionBridge) {
            this@toBridge.update(action)
        }
    }
