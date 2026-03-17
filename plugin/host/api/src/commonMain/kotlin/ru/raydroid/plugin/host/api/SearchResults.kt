package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems

data class SearchResults(
    val cachedResults: List<Item>,
    val content: Map<SinglePluginRuntime, List<RayItems>>
) {
    data class Item(
        val listItemId: ListItemId,
        val item: ListItem,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    )
}
