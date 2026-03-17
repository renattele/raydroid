package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ItemId

data class ListItemId(
    val pluginId: PluginId,
    val commandName: String,
    val itemId: ItemId
)