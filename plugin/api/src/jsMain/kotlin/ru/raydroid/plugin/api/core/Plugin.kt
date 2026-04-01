package ru.raydroid.plugin.api.core

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.RayNodeData
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
            requestedItems: List<ItemId>?,
            chunkSize: Int
        ): Flow<List<ListItem>> {
            return this@toBridge.cachedItems(requestedItems, chunkSize)
        }

        override fun content(): RayItems {
            val items = mutableMapOf<ItemId, RayItem>()
            val scope = object : RayListScope {
                override fun item(
                    id: ItemId,
                    title: UiText?,
                    description: UiText?,
                    icon: Icon?,
                    actions: RayListActionScope.() -> Unit,
                    content: RayScope.() -> Unit
                ) {
                    val actionsList = buildActions(group = null, actions)
                    val listItem = ListItem(
                        id = id,
                        title = title,
                        description = description,
                        icon = icon,
                        actions = actionsList
                    )
                    items[id] = RayItem(
                        listItem = listItem,
                        content = buildRayNodes(content)
                    )
                }
            }
            scope.content()
            return items
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
            content: RayListActionScope.() -> Unit
        ): List<ListItemAction> {
            val actions = mutableListOf<ListItemAction>()
            val scope = object : RayListActionScope {
                override fun group(
                    title: UiText,
                    content: RayListActionScope.() -> Unit
                ) {
                    actions += buildActions(title, content)
                }

                override fun action(
                    id: String,
                    title: UiText,
                    icon: Icon?,
                    description: UiText?,
                    style: ListItemAction.Style,
                    primary: Boolean
                ) {
                    actions += ListItemAction(
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
