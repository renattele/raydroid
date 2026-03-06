package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.core.ItemId

interface SearchRepository {
    suspend fun update(updateList: List<ListItemUpdate>)
    suspend fun updateUsage(pluginId: PluginId, commandName: String, itemId: ItemId)
    fun search(query: String, limit: Int = 50): Flow<List<SearchResults.Item>>
}
