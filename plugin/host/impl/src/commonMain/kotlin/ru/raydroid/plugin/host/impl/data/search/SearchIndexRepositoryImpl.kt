package ru.raydroid.plugin.host.impl.data.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheContentEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheDao
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheMutation
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheSearchEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheWithContent

internal class SearchIndexRepositoryImpl(
    private val cacheDao: SearchIndexCacheDao,
    private val resourceResolver: SearchResourceResolver,
    private val clock: Clock,
    private val ranker: SearchRanker
) : SearchIndexRepository {
    override suspend fun update(mutations: List<SearchIndexMutation>) {
        if (mutations.isEmpty()) return
        val session = resourceResolver.session()
        val resolvedMutations = mutations.map { mutation ->
            mutation.toCacheMutation(session)
        }
        cacheDao.applyUpdates(resolvedMutations)
    }

    override suspend fun updateUsage(resultId: SearchResultId) {
        cacheDao.updateUsage(
            pluginId = resultId.pluginId.id,
            command = resultId.commandName,
            itemId = resultId.itemId.value,
            nowEpochMs = clock.now().toEpochMilliseconds()
        )
    }

    override fun search(query: String, limit: Int): Flow<List<SearchResultSet.SearchResult>> {
        val normalizedQuery = NormalizedText.from(query)
        if (normalizedQuery.text.isBlank()) {
            return cacheDao.recent(limit).map { recentResults ->
                recentResults.map { result ->
                    result.toSearchResult(
                        titleMatches = emptyList(),
                        descriptionMatches = emptyList()
                    )
                }
            }
        }

        val ftsLimit = min(max(limit * FTS_CANDIDATE_MULTIPLIER, FTS_CANDIDATE_MINIMUM), MAX_CANDIDATES)
        return combine(
            cacheDao.searchFtsCandidates(
                matchQuery = normalizedQuery.toFtsMatchQuery(),
                limit = ftsLimit
            ),
            cacheDao.searchFallbackCandidates(limit = MAX_CANDIDATES)
        ) { ftsCandidates, fallbackCandidates ->
            ranker.rank(
                query = normalizedQuery,
                ftsCandidates = ftsCandidates,
                fallbackCandidates = fallbackCandidates,
                limit = limit,
                nowEpochMs = clock.now().toEpochMilliseconds()
            )
        }
    }

    private suspend fun SearchIndexMutation.Upsert.toCacheEntity(
        session: SearchResourceResolver.Session
    ): SearchIndexCacheWithContent {
        val resolvedIcon = session.resolveIcon(resultId.pluginId, listEntry.icon)
        val resolvedContent = session.resolveContent(
            pluginId = resultId.pluginId,
            title = listEntry.title,
            description = listEntry.description
        )
        return SearchIndexCacheWithContent(
            searchIndexCache = SearchIndexCacheEntity(
                pluginId = resultId.pluginId.id,
                command = resultId.commandName,
                itemId = listEntry.id.value,
                icon = resolvedIcon?.value,
                iconType = resolvedIcon?.type?.name
            ),
            content = resolvedContent.map { content ->
                SearchIndexCacheContentEntity(
                    title = content.title,
                    description = content.description
                )
            }
        )
    }

    private suspend fun SearchIndexMutation.toCacheMutation(
        session: SearchResourceResolver.Session
    ): SearchIndexCacheMutation {
        return when (this) {
            is SearchIndexMutation.Upsert -> SearchIndexCacheMutation.Upsert(toCacheEntity(session))
            is SearchIndexMutation.Delete -> SearchIndexCacheMutation.Delete(
                pluginId = resultId.pluginId.id,
                commandName = resultId.commandName,
                itemId = resultId.itemId.value
            )
            is SearchIndexMutation.ClearOutdated -> SearchIndexCacheMutation.ClearOutdated(
                pluginId = pluginId.id,
                commandName = commandName
            )
            is SearchIndexMutation.MarkAllAsOutdated -> SearchIndexCacheMutation.MarkAllAsOutdated(
                pluginId = pluginId.id,
                commandName = commandName
            )
        }
    }

    private fun SearchIndexCacheSearchEntity.toSearchResult(
        titleMatches: List<IntRange>,
        descriptionMatches: List<IntRange>
    ): SearchResultSet.SearchResult {
        return SearchResultSet.CachedSearchResult(
            resultId = SearchResultId(
                pluginId = PluginId(pluginId),
                commandName = command,
                itemId = CommandItemId(itemId)
            ),
            listEntry = CommandListItem(
                id = CommandItemId(itemId),
                icon = icon?.let { iconValue ->
                    iconType?.let { Icon(iconValue, Icon.Type.valueOf(it)) }
                },
                title = title?.let(UiText::Plain),
                description = description?.let(UiText::Plain)
            ),
            titleMatches = titleMatches,
            descriptionMatches = descriptionMatches
        )
    }

    private companion object {
        const val FTS_CANDIDATE_MULTIPLIER = 8
        const val FTS_CANDIDATE_MINIMUM = 64
        const val MAX_CANDIDATES = 512
    }
}
