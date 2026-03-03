package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem

sealed class ListItemUpdate {
    data class Upsert(
        val pluginId: PluginId,
        val commandName: String,
        val item: ListItem
    ) : ListItemUpdate()

    data class Delete(
        val pluginId: PluginId,
        val commandName: String,
        val itemId: ItemId
    ) : ListItemUpdate()
}