package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems

interface CommandRepository {
    suspend fun cachedItems(pluginId: PluginId): List<ListItem>
    fun content(pluginId: PluginId): RayItems

    fun initialize(pluginId: PluginId, onRenderRequest: () -> Unit)

    suspend fun update(pluginId: PluginId, query: String, action: CommandAction)
}