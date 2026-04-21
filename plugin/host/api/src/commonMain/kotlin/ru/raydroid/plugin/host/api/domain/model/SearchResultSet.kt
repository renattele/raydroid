package ru.raydroid.plugin.host.api.domain.model

import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation

data class SearchResultSet(
    val results: List<SearchResult>,
) {
    sealed interface SearchResult {
        val resultId: SearchResultId
        val listEntry: PluginCommandListItem
    }

    data class CachedSearchResult(
        override val resultId: SearchResultId,
        override val listEntry: PluginCommandListItem,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    ) : SearchResult

    data class CommandSearchResult(
        override val resultId: SearchResultId,
        override val listEntry: PluginCommandListItem
    ) : SearchResult

    data class LiveSearchResult(
        override val resultId: SearchResultId,
        override val listEntry: PluginCommandListItem,
        val presentation: PluginCommandPresentation
    ) : SearchResult
}
