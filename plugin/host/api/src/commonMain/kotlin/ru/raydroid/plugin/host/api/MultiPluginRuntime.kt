package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItem
import ru.raydroid.plugin.api.core.RayItems

interface MultiPluginRuntime {
    fun cachedItems(): Flow<Map<SinglePluginRuntime, List<ListItemUpdate>>>

    fun runtimes(): StateFlow<List<SinglePluginRuntime>>

    fun content(): StateFlow<List<ContentItem>>

    suspend fun update(query: String, action: CommandAction)

    data class ContentItem(
        val runtime: SinglePluginRuntime,
        val item: RayItem,
        val listItem: ListItem,
        val listItemId: ListItemId
    )
}
