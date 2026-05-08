package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.api.presentation.CommandItemId

data class PluginFocusableItem(
    val id: CommandItemId,
    val actions: List<PluginCommandListAction>
)

data class PluginFocusModel(
    val focusedItemId: CommandItemId?,
    val items: List<PluginFocusableItem>
) {
    val focusedItem: PluginFocusableItem?
        get() = focusedItemId?.let { id -> items.firstOrNull { item -> item.id == id } }

    val focusedActions: List<PluginCommandListAction>
        get() = focusedItem?.actions.orEmpty()

}

fun List<PluginRayNodeData>.pluginFocusModel(
    focusedItemId: CommandItemId?,
    query: String
): PluginFocusModel {
    val items = pluginFocusableItems(query)
    val resolvedFocusedItemId = focusedItemId
        ?.takeIf { id -> items.any { item -> item.id == id } }
        ?: items.firstOrNull()?.id
    return PluginFocusModel(
        focusedItemId = resolvedFocusedItemId,
        items = items
    )
}

fun List<PluginRayNodeData>.pluginFocusableItems(query: String): List<PluginFocusableItem> =
    flatMap { node -> node.pluginFocusableItems(query) }

fun List<PluginRayNodeData>.suppressesHostActions(): Boolean =
    any { node -> node.suppressesHostActions() }

fun List<PluginRayNodeData>.actionPanelHintMode(): PluginActionPanelHintMode =
    firstNotNullOfOrNull { node -> node.actionPanelHintModeOrNull() }
        ?: PluginActionPanelHintMode.Full

private fun PluginRayNodeData.pluginFocusableItems(query: String): List<PluginFocusableItem> =
    when (this) {
        is PluginListData -> filtered(query).flatMap { section ->
            section.items.mapNotNull { item ->
                item.takeIf { it.itemModifier?.enabled != false }?.let {
                    PluginFocusableItem(
                        id = it.id,
                        actions = it.itemModifier?.actions.orEmpty()
                    )
                }
            }
        }
        is PluginGridData -> filtered(query).flatMap { section ->
            section.items.mapNotNull { item ->
                item.takeIf { it.itemModifier?.enabled != false }?.let {
                    PluginFocusableItem(
                        id = it.id,
                        actions = it.itemModifier?.actions.orEmpty()
                    )
                }
            }
        }
        is PluginBoxData -> children.pluginFocusableItems(query)
        is PluginOrientedBoxData -> children.pluginFocusableItems(query)
        is PluginDetailData,
        is PluginFormData,
        is PluginIconData,
        is PluginImageData,
        is PluginTextData -> emptyList()
    }

private fun PluginRayNodeData.suppressesHostActions(): Boolean =
    when (this) {
        is PluginFormData -> suppressHostActions
        is PluginBoxData -> children.suppressesHostActions()
        is PluginOrientedBoxData -> children.suppressesHostActions()
        is PluginDetailData,
        is PluginGridData,
        is PluginIconData,
        is PluginImageData,
        is PluginListData,
        is PluginTextData -> false
    }

private fun PluginRayNodeData.actionPanelHintModeOrNull(): PluginActionPanelHintMode? =
    when (this) {
        is PluginFormData -> actionPanelHintMode
        is PluginBoxData -> children.firstNotNullOfOrNull { node -> node.actionPanelHintModeOrNull() }
        is PluginOrientedBoxData -> children.firstNotNullOfOrNull { node -> node.actionPanelHintModeOrNull() }
        is PluginDetailData,
        is PluginGridData,
        is PluginIconData,
        is PluginImageData,
        is PluginListData,
        is PluginTextData -> null
    }

private fun PluginListData.filtered(query: String): List<PluginListSectionData> {
    if (!filtering || query.isBlank()) return sections
    return sections.map { section ->
        section.copy(items = section.items.filter { item -> item.matches(query) })
    }
}

private fun PluginGridData.filtered(query: String): List<PluginGridSectionData> {
    if (!filtering || query.isBlank()) return sections
    return sections.map { section ->
        section.copy(items = section.items.filter { item -> item.matches(query) })
    }
}

private fun PluginListItemData.matches(query: String): Boolean {
    val needle = query.lowercase()
    return title.searchText().contains(needle) ||
        subtitle?.searchText()?.contains(needle) == true ||
        keywords.any { it.lowercase().contains(needle) }
}

private fun PluginGridItemData.matches(query: String): Boolean {
    val needle = query.lowercase()
    return title.searchText().contains(needle) ||
        subtitle?.searchText()?.contains(needle) == true ||
        keywords.any { it.lowercase().contains(needle) }
}

private fun PluginUiText.searchText(): String = when (this) {
    is PluginUiText.Plain -> text
    is PluginUiText.Resource -> key
}.lowercase()
