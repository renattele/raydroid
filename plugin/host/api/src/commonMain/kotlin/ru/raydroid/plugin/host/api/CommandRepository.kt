package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems

interface CommandRepository {
    suspend fun cachedItems(
        pluginId: PluginId,
        requestedItems: List<ItemId>? = null,
        chunkSize: Int = 100
    ): Flow<List<ListItem>>
    fun content(pluginId: PluginId): RayItems

    fun initialize(pluginId: PluginId, onRenderRequest: () -> Unit)

    suspend fun update(pluginId: PluginId, query: String, action: CommandAction)
}
