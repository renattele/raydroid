package ru.raydroid.feature.search

import ru.raydroid.plugin.host.api.ui.ActionPanelActionUi
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginUiText

data class SearchPanelAction(
    override val title: PluginUiText,
    override val description: PluginUiText? = null,
    override val icon: PluginIcon? = null,
    override val group: PluginUiText? = null,
    override val primary: Boolean = false,
    override val enabled: Boolean = true,
    override val destructive: Boolean = false,
    val kind: Kind
) : ActionPanelActionUi {
    sealed interface Kind {
        data class PluginCallback(
            val callback: ru.raydroid.plugin.host.api.ui.PluginCommandCallback,
            val updateUsage: Boolean
        ) : Kind

        data class OpenAliasEditor(
            val existingAlias: String?
        ) : Kind

        data object RemoveAlias : Kind
    }
}

fun PluginCommandListAction.toSearchPanelAction(updateUsage: Boolean): SearchPanelAction {
    return SearchPanelAction(
        title = title,
        description = description,
        icon = icon,
        group = group,
        primary = primary,
        enabled = enabled,
        destructive = style == PluginCommandListAction.Style.Destructive,
        kind = SearchPanelAction.Kind.PluginCallback(
            callback = callback,
            updateUsage = updateUsage
        )
    )
}

fun List<PluginCommandListAction>.toSearchPanelActions(updateUsage: Boolean): List<SearchPanelAction> {
    return map { action -> action.toSearchPanelAction(updateUsage) }
}
