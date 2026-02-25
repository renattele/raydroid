package ru.raydroid.plugin.api.core

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
            zipline.bind(command::class.simpleName!!, command.toBridge())
        }
    }
    scope.content()
}

internal fun CommandService.toBridge(): CommandServiceBridge = object : CommandServiceBridge {
    override suspend fun cachedItems(): List<ListItem> {
        return this@toBridge.cachedItems()
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

    override fun initialize(request: CommandServiceBridge.RenderRequest) {
        onRenderRequest = request::requestRender
    }

    override suspend fun update(query: String, action: CommandAction) {
        this@toBridge.update(query, action)
    }
}