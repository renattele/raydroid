package ru.raydroid.feature.search

import ru.raydroid.plugin.host.api.ui.PluginUiText

data class CommandCatalogEntry(
    val id: String,
    val title: PluginUiText,
)

data class ResolvedCommandCatalogEntry(
    val id: String,
    val title: String,
)
