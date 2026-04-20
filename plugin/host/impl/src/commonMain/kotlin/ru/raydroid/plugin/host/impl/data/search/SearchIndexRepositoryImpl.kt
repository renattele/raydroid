package ru.raydroid.plugin.host.impl.data.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheContentEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheDao
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheMutation
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> {
        val normalizedQuery = SearchQueryNormalizer.from(query)
        if (normalizedQuery.isBlank) {
            return cacheDao.recent(limit).map { recentResults ->
                ranker.rankRecent(
                    candidates = recentResults,
                    limit = limit,
                    nowEpochMs = clock.now().toEpochMilliseconds()
                )
            }
        }

        val ftsLimit = min(max(limit * FTS_CANDIDATE_MULTIPLIER, FTS_CANDIDATE_MINIMUM), MAX_CANDIDATES)
        val fallbackLimit = if (normalizedQuery.isShort) {
            SHORT_QUERY_FALLBACK_CANDIDATES
        } else {
            FUZZY_FALLBACK_CANDIDATES
        }
        val strictCandidatesFlow = normalizedQuery.strictFtsQuery?.let { matchQuery ->
            cacheDao.searchFtsCandidates(matchQuery = matchQuery, limit = ftsLimit)
        } ?: flowOf(emptyList())

        return strictCandidatesFlow.flatMapLatest { strictCandidates ->
            val strictResults = ranker.rankCached(
                query = normalizedQuery,
                ftsCandidates = strictCandidates,
                fallbackCandidates = emptyList(),
                limit = limit,
                nowEpochMs = clock.now().toEpochMilliseconds()
            )
            val needsRelaxed = strictResults.size < limit && normalizedQuery.relaxedFtsQuery != null
            val needsFallback = normalizedQuery.isShort || strictResults.size < limit

            if (!needsRelaxed && !needsFallback) {
                flowOf(strictResults)
            } else {
                combine(
                    if (needsRelaxed) {
                        cacheDao.searchFtsCandidates(
                            matchQuery = normalizedQuery.relaxedFtsQuery.orEmpty(),
                            limit = ftsLimit
                        )
                    } else {
                        flowOf(emptyList())
                    },
                    if (needsFallback) {
                        cacheDao.searchFallbackCandidates(limit = fallbackLimit)
                    } else {
                        flowOf(emptyList())
                    }
                ) { relaxedCandidates, fallbackCandidates ->
                    ranker.rankCached(
                        query = normalizedQuery,
                        ftsCandidates = strictCandidates + relaxedCandidates,
                        fallbackCandidates = fallbackCandidates,
                        limit = limit,
                        nowEpochMs = clock.now().toEpochMilliseconds()
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
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
                val titleSearch = SearchQueryNormalizer.searchable(content.title)
                val descriptionSearch = SearchQueryNormalizer.searchable(content.description)
                SearchIndexCacheContentEntity(
                    title = content.title,
                    description = content.description,
                    titleSearch = titleSearch,
                    descriptionSearch = descriptionSearch,
                    acronymSearch = listOf(
                        SearchQueryNormalizer.acronym(content.title),
                        SearchQueryNormalizer.acronym(content.description)
                    ).filter { it.isNotBlank() }.joinToString(" ")
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

    private companion object {
        const val FTS_CANDIDATE_MULTIPLIER = 8
        const val FTS_CANDIDATE_MINIMUM = 64
        const val MAX_CANDIDATES = 192
        const val SHORT_QUERY_FALLBACK_CANDIDATES = 64
        const val FUZZY_FALLBACK_CANDIDATES = 48
    }
}
