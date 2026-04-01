package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItem

data class SearchResults(
    val results: List<Item>,
) {
    sealed interface Item {
        val item: ListItem
        val listItemId: ListItemId
    }

    data class CachedItem(
        override val listItemId: ListItemId,
        override val item: ListItem,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    ) : Item

    data class ItemWithContent(
        override val listItemId: ListItemId,
        override val item: ListItem,
        val runtime: SinglePluginRuntime,
        val rayItem: RayItem
    ) : Item
}
