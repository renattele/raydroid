package ru.raydroid.plugin.host.impl

import app.cash.zipline.Zipline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.api.Plugin
import ru.raydroid.plugin.host.api.SinglePluginRuntime

class SinglePluginRuntimeImpl(
    override val manifest: Manifest,
    override val resources: FileSystem,
    private val commandServices: List<CommandServiceBridge>,
    private val zipline: Zipline
) : SinglePluginRuntime {
    private val contentFlow = MutableStateFlow<List<RayItems>>(List(commandServices.size) {
        emptyMap()
    })

    init {
        commandServices.forEachIndexed { index, command ->
            command.initialize(object : CommandServiceBridge.RenderRequest {
                override fun requestRender() {
                    contentFlow.update { data ->
                        val updated = data.toMutableList()
                        updated[index] = command.content()
                        updated
                    }
                }
            })
        }
    }

    override suspend fun cachedItems(): List<ListItem> {
        return commandServices.flatMap { service ->
            service.cachedItems()
        }
    }

    override fun content(): StateFlow<List<RayItems>> = contentFlow

    override suspend fun update(
        query: String, action: CommandAction
    ) {
        commandServices.forEach { command ->
            command.update(query, action)
        }
    }

    override suspend fun unload() {
        zipline.close()
    }
}