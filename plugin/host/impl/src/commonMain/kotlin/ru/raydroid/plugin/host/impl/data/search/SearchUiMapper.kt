package ru.raydroid.plugin.host.impl.data.search

import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultScore
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandListQuickAction
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheSearchEntity

internal fun SearchIndexCacheSearchEntity.toPluginListEntry(): PluginCommandListItem {
    val pluginId = PluginId(pluginId)
    return PluginCommandListItem(
        id = CommandItemId(itemId),
        icon = icon?.toPluginIcon(pluginId, iconType),
        iconColor = iconColor?.toPluginColor()
            ?: PluginColor.OnSurfaceVariant.takeIf { iconType == Icon.Type.Builtin.name },
        title = title?.let(PluginUiText::Plain),
        description = description?.let(PluginUiText::Plain),
        alias = null,
        quickAction = quickAction()
    )
}

private fun String.toPluginColor(): PluginColor? =
    PluginColor.entries.firstOrNull { color -> color.name == this }

private fun String.toPluginIcon(
    pluginId: PluginId,
    iconTypeName: String?
): PluginIcon? {
    val iconType = Icon.Type.entries.firstOrNull { type -> type.name == iconTypeName } ?: return null
    return when (iconType) {
        Icon.Type.Url -> PluginIcon.Url(this)
        Icon.Type.Resource -> PluginIcon.Resource(pluginId = pluginId, key = this)
        Icon.Type.Base64 -> PluginIcon.Base64(this)
        Icon.Type.Builtin -> PluginIcon.Builtin(this)
    }
}

private fun SearchIndexCacheSearchEntity.quickAction(): PluginCommandListQuickAction? {
    if (pluginId != ContactsPluginId || command != ContactsCommandName) return null
    return PluginCommandListQuickAction(
        title = PluginUiText.Plain("Call"),
        icon = PluginIcon.Builtin("Call")
    )
}

internal fun SearchIndexCacheSearchEntity.toRankedPreview(): RankedSearchResult {
    return RankedSearchResult(
        result = SearchResultSet.CachedSearchResult(
            resultId = SearchResultId(
                pluginId = PluginId(pluginId),
                commandName = command,
                itemId = CommandItemId(itemId)
            ),
            listEntry = toPluginListEntry(),
            titleMatches = emptyList(),
            descriptionMatches = emptyList()
        ),
        score = SearchResultScore(textScore = 0.0, stableOrder = contentId)
    )
}

private const val ContactsPluginId = "ru.raydroid.plugin.impl.contacts"
private const val ContactsCommandName = "ContactsCommand"
