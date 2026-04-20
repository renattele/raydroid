package ru.raydroid.plugin.api.runtime

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.presentation.CommandPresentationMap
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.buildRayNodes
import ru.raydroid.plugin.api.zipline

@DslMarker
annotation class PluginMarker

interface PluginScope {
    @PluginMarker
    fun register(command: CommandService)
}

@PluginMarker
fun plugin(content: PluginScope.() -> Unit) {
    val scope = object : PluginScope {
        override fun register(command: CommandService) {
            val serviceName = command::class.simpleName!!
            zipline.bind(serviceName, command.toBridge(serviceName))
        }
    }
    scope.content()
}

internal fun CommandService.toBridge(serviceName: String): CommandServiceBridge =
    object : CommandServiceBridge {
        override fun getServiceName() = serviceName

        override suspend fun cachedItems(
            requestedItems: List<CommandItemId>?,
            chunkSize: Int
        ): Flow<List<CommandListItem>> {
            return this@toBridge.cachedItems(requestedItems, chunkSize)
        }

        override fun content(): CommandPresentationMap {
            val presentations = mutableMapOf<CommandItemId, CommandPresentation>()
            val scope = object : CommandListScope {
                override fun entry(
                    id: CommandItemId,
                    title: UiText?,
                    description: UiText?,
                    icon: Icon?,
                    actions: CommandActionScope.() -> Unit,
                    content: RayScope.() -> Unit
                ) {
                    val actionList = buildActions(group = null, actions)
                    val listEntry = CommandListItem(
                        id = id,
                        title = title,
                        description = description,
                        icon = icon,
                        actions = actionList,
                    )
                    presentations[id] = CommandPresentation(
                        listEntry = listEntry,
                        content = buildRayNodes(content)
                    )
                }
            }
            scope.content()
            return presentations
        }

        override fun initialize(
            request: CommandServiceBridge.RenderRequest,
            invalidateCacheRequest: CommandServiceBridge.InvalidateCacheRequest
        ) {
            onRenderRequest = request::requestRender
            onInvalidateCacheRequest = invalidateCacheRequest::requestInvalidation
        }

        override suspend fun update(query: String, action: CommandAction) {
            this@toBridge.update(query, action)
        }

        private fun buildActions(
            group: UiText?,
            content: CommandActionScope.() -> Unit
        ): List<CommandListAction> {
            val actions = mutableListOf<CommandListAction>()
            val scope = object : CommandActionScope {
                override fun group(
                    title: UiText,
                    content: CommandActionScope.() -> Unit
                ) {
                    actions += buildActions(title, content)
                }

                override fun action(
                    id: CommandActionId,
                    title: UiText,
                    icon: Icon?,
                    description: UiText?,
                    style: CommandListAction.Style,
                    primary: Boolean
                ) {
                    actions += CommandListAction(
                        id = id,
                        title = title,
                        icon = icon,
                        description = description,
                        group = group,
                        style = style,
                        primary = primary
                    )
                }
            }
            scope.content()
            return actions
        }
    }
