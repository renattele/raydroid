package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.SearchRepository
import ru.raydroid.plugin.host.api.SearchResults
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheDao
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheContentEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheMutation
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheSearchEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheWithContent
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock

internal class SearchRepositoryImpl(
    private val cacheDao: ListItemCacheDao,
    private val resourceResolver: SearchResourceResolver,
    private val clock: Clock,
    private val ranker: SearchRanker
) : SearchRepository {
    override suspend fun update(updateList: List<ListItemUpdate>) {
        if (updateList.isEmpty()) return
        val session = resourceResolver.session()
        val resolvedMutations = updateList.map { updateItem ->
            updateItem.toCacheMutation(session)
        }
        cacheDao.applyUpdates(resolvedMutations)
    }

    override suspend fun updateUsage(listItemId: ListItemId) {
        cacheDao.updateUsage(
            pluginId = listItemId.pluginId.id,
            command = listItemId.commandName,
            itemId = listItemId.itemId.value,
            nowEpochMs = clock.now().toEpochMilliseconds()
        )
    }

    override fun search(
        query: String,
        limit: Int
    ): Flow<List<SearchResults.Item>> {
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

    private suspend fun ListItemUpdate.Upsert.toCacheEntity(
        session: SearchResourceResolver.Session
    ): ListItemCacheWithContent {
        val resolvedIcon = session.resolveIcon(listItemId.pluginId, item.icon)
        val resolvedContent = session.resolveContent(
            pluginId = listItemId.pluginId,
            title = item.title,
            description = item.description
        )
        return ListItemCacheWithContent(
            listItemCache = ListItemCacheEntity(
                pluginId = listItemId.pluginId.id,
                command = listItemId.commandName,
                itemId = item.id.value,
                icon = resolvedIcon.value,
                iconType = resolvedIcon.type.name
            ),
            content = resolvedContent.map { content ->
                ListItemCacheContentEntity(
                    title = content.title,
                    description = content.description
                )
            }
        )
    }

    private suspend fun ListItemUpdate.toCacheMutation(
        session: SearchResourceResolver.Session
    ): ListItemCacheMutation {
        return when (this) {
            is ListItemUpdate.Upsert -> ListItemCacheMutation.Upsert(toCacheEntity(session))
            is ListItemUpdate.Delete -> ListItemCacheMutation.Delete(
                pluginId = listItemId.pluginId.id,
                commandName = listItemId.commandName,
                itemId = listItemId.itemId.value
            )
            is ListItemUpdate.ClearOutdated -> ListItemCacheMutation.ClearOutdated(
                pluginId = pluginId.id,
                commandName = commandName
            )
            is ListItemUpdate.MarkAllAsOutdated -> ListItemCacheMutation.MarkAllAsOutdated(
                pluginId = pluginId.id,
                commandName = commandName
            )
        }
    }

    private fun ListItemCacheSearchEntity.toSearchResult(
        titleMatches: List<IntRange>,
        descriptionMatches: List<IntRange>
    ): SearchResults.Item {
        return SearchResults.Item(
            listItemId = ListItemId(
                pluginId = PluginId(pluginId),
                commandName = command,
                itemId = ItemId(itemId)
            ),
            item = ListItem(
                id = ItemId(itemId),
                icon = Icon(icon, Icon.Type.valueOf(iconType)),
                title = ru.raydroid.plugin.api.core.UiText.Plain(title),
                description = ru.raydroid.plugin.api.core.UiText.Plain(description)
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
