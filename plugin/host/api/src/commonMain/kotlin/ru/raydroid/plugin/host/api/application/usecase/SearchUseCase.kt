package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ru.raydroid.plugin.host.api.domain.model.SearchAliasNormalizer
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker

class SearchUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val searchIndexRepository: SearchIndexRepository,
    private val searchResultRanker: SearchResultRanker,
    private val searchAliasRepository: SearchAliasRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(query: String, limit: Int = 50): Flow<SearchResultSet> {
        val runtimeCoordinator = pluginRuntimeRegistry.get()
        val commandsFlow = runtimeCoordinator.commands()
        val contentFlow = runtimeCoordinator.content()
        val cachedResultsFlow = searchIndexRepository.search(query, limit)
        val aliasesFlow = searchAliasRepository.observeAliases()

        return channelFlow {
            launch {
                runCatching {
                    runtimeCoordinator.update(
                        CommandActionBridge.Regular(CommandAction.Type(query))
                    )
                }
            }

            combine(commandsFlow, contentFlow, cachedResultsFlow, aliasesFlow) { commandItems, liveResults, cachedResults, aliases ->
                val rankedCommandResults = searchResultRanker.rankCommands(
                    query = query,
                    commandsSnapshot = commandItems,
                    limit = limit
                )
                val rankedLiveResults = searchResultRanker.rankLive(
                    query = query,
                    contentSnapshot = liveResults,
                    limit = limit
                )
                SearchSnapshot(
                    results = searchResultRanker.merge(
                        commandResults = rankedCommandResults,
                        liveResults = rankedLiveResults,
                        cachedResults = cachedResults,
                        limit = limit
                    ).decorateAliases(aliases),
                    commandItems = commandItems,
                    aliases = aliases
                )
            }.flowOn(Dispatchers.Default).collectLatest { snapshot ->
                send(
                    SearchResultSet(
                        results = promoteExactAlias(
                            query = query,
                            snapshot = snapshot,
                            limit = limit
                        )
                    )
                )
            }
        }
    }

    private suspend fun promoteExactAlias(
        query: String,
        snapshot: SearchSnapshot,
        limit: Int
    ): List<SearchResultSet.SearchResult> {
        val runtimeCoordinator = pluginRuntimeRegistry.get()
        val aliasResultId = SearchAliasNormalizer.normalizeLookup(query)
            ?.let { alias -> snapshot.aliases.entries.firstOrNull { entry -> entry.value == alias }?.key }
            ?: return snapshot.results
        val promoted = snapshot.results.firstOrNull { result -> result.resultId == aliasResultId }
            ?: snapshot.commandItems.firstOrNull { item -> item.resultId == aliasResultId }?.toCommandSearchResult(
                alias = snapshot.aliases[aliasResultId]
            )
            ?: searchIndexRepository.getPreview(aliasResultId)?.result?.decorateAlias(snapshot.aliases[aliasResultId])
            ?: refreshCachedAliasResult(runtimeCoordinator, aliasResultId)?.decorateAlias(snapshot.aliases[aliasResultId])
            ?: return snapshot.results
        return listOf(promoted) + snapshot.results
            .filterNot { result -> result.resultId == aliasResultId }
            .take((limit - 1).coerceAtLeast(0))
    }

    private suspend fun refreshCachedAliasResult(
        runtimeCoordinator: PluginRuntimeCoordinator,
        resultId: SearchResultId
    ): SearchResultSet.SearchResult? {
        val runtime = runtimeCoordinator.runtimes().value.firstOrNull { candidate ->
            candidate.pluginId == resultId.pluginId
        } ?: return null
        val refreshedMutations = runtime.cachedItems(
            commandName = resultId.commandName,
            requestedItems = listOf(resultId.itemId)
        )
        if (refreshedMutations.isEmpty()) return null
        searchIndexRepository.update(refreshedMutations)
        return searchIndexRepository.getPreview(resultId)?.result
    }
}

private data class SearchSnapshot(
    val results: List<SearchResultSet.SearchResult>,
    val commandItems: List<PluginRuntimeCoordinator.CommandItem>,
    val aliases: Map<SearchResultId, String>
)

private fun List<SearchResultSet.SearchResult>.decorateAliases(
    aliases: Map<SearchResultId, String>
): List<SearchResultSet.SearchResult> = map { result ->
    result.decorateAlias(aliases[result.resultId])
}

private fun SearchResultSet.SearchResult.decorateAlias(alias: String?): SearchResultSet.SearchResult {
    return when (this) {
        is SearchResultSet.CachedSearchResult -> copy(
            listEntry = listEntry.copy(alias = alias)
        )

        is SearchResultSet.CommandSearchResult -> copy(
            listEntry = listEntry.copy(alias = alias)
        )

        is SearchResultSet.LiveSearchResult -> copy(
            listEntry = listEntry.copy(alias = alias)
        )
    }
}

private fun PluginRuntimeCoordinator.CommandItem.toCommandSearchResult(alias: String?): SearchResultSet.CommandSearchResult {
    return SearchResultSet.CommandSearchResult(
        resultId = resultId,
        listEntry = listEntry.copy(alias = alias)
    )
}
