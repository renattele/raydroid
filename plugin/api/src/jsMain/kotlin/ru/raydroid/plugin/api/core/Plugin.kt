package ru.raydroid.plugin.api.core

import kotlinx.coroutines.flow.Flow
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

internal fun CommandService.toBridge(serviceName: String): CommandServiceBridge = object : CommandServiceBridge {
    override val serviceName = serviceName

    override suspend fun cachedItems(requestedItems: List<ItemId>?, chunkSize: Int): Flow<List<ListItem>> {
        return this@toBridge.cachedItems(requestedItems, chunkSize)
    }

    override fun content(): RayItems {
        val items = mutableMapOf<ItemId, List<RayNodeData>>()
        val scope = object : RayListScope {
            override fun item(
                id: ItemId,
                content: RayScope.() -> Unit
            ) {
                items[id] = buildRayNodes(content)
            }
        }
        scope.content()
        return items
    }

    override fun initialize(request: CommandServiceBridge.RenderRequest,
                            invalidateCacheRequest: CommandServiceBridge.InvalidateCacheRequest) {
        onRenderRequest = request::requestRender
        onInvalidateCacheRequest = invalidateCacheRequest::requestInvalidation
    }

    override suspend fun update(query: String, action: CommandAction) {
        this@toBridge.update(query, action)
    }
}
