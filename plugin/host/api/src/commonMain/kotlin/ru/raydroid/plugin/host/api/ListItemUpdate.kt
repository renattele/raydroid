package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ListItem

sealed class ListItemUpdate {
    data class Upsert(
        val listItemId: ListItemId,
        val item: ListItem
    ) : ListItemUpdate()

    data class Delete(
        val listItemId: ListItemId
    ) : ListItemUpdate()

    data class Clear(
        val pluginId: PluginId,
        val commandName: String
    ) : ListItemUpdate()
}
