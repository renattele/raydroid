package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems

data class SearchResults(
    private val cachedResults: List<ListItem>,
    private val content: Map<SinglePluginRuntime, List<RayItems>>
)