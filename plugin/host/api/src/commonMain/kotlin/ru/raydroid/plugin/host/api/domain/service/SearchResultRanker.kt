package ru.raydroid.plugin.host.api.domain.service

import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator

interface SearchResultRanker {
    fun rankLive(
        query: String,
        contentSnapshot: List<PluginRuntimeCoordinator.ContentItem>,
        limit: Int,
    ): List<RankedSearchResult>

    fun rankCommands(
        query: String,
        commandsSnapshot: List<PluginRuntimeCoordinator.CommandItem>,
        limit: Int,
    ): List<RankedSearchResult>

    fun merge(
        commandResults: List<RankedSearchResult>,
        liveResults: List<RankedSearchResult>,
        cachedResults: List<RankedSearchResult>,
        limit: Int,
    ): List<SearchResultSet.SearchResult>
}
