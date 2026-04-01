package ru.raydroid.plugin.host.api.domain.model

import ru.raydroid.plugin.api.presentation.CommandItemId

data class SearchResultId(
    val pluginId: PluginId,
    val commandName: String,
    val itemId: CommandItemId
)