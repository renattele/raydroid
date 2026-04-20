package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandItemId

data class PluginCommandListItem(
    val id: CommandItemId,
    val icon: PluginIcon?,
    val title: PluginUiText?,
    val description: PluginUiText?,
    val actions: List<PluginCommandListAction> = emptyList(),
)

data class PluginCommandListAction(
    val id: CommandActionId,
    val title: PluginUiText,
    val description: PluginUiText?,
    val icon: PluginIcon?,
    val group: PluginUiText? = null,
    val style: Style = Style.Default,
    val primary: Boolean = false
) {
    enum class Style {
        Default,
        Destructive
    }
}

data class PluginCommandPresentation(
    val listEntry: PluginCommandListItem,
    val content: List<PluginRayNodeData>
)
