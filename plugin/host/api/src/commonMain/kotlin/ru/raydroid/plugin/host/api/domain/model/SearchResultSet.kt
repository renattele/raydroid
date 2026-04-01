package ru.raydroid.plugin.host.api.domain.model

import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime

data class SearchResultSet(
    val results: List<SearchResult>,
) {
    sealed interface SearchResult {
        val resultId: SearchResultId
        val listEntry: CommandListItem
    }

    data class CachedSearchResult(
        override val resultId: SearchResultId,
        override val listEntry: CommandListItem,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    ) : SearchResult

    data class LiveSearchResult(
        override val resultId: SearchResultId,
        override val listEntry: CommandListItem,
        val runtime: PluginRuntime,
        val presentation: CommandPresentation
    ) : SearchResult
}
