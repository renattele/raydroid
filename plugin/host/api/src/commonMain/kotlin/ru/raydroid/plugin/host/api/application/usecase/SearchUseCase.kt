package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker

class SearchUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val searchIndexRepository: SearchIndexRepository,
    private val searchResultRanker: SearchResultRanker
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(query: String, limit: Int = 50): Flow<SearchResultSet> {
        val runtimeCoordinator = pluginRuntimeRegistry.get()
        val contentFlow = runtimeCoordinator.content()
        val cachedResultsFlow = searchIndexRepository.search(query, limit)

        return channelFlow {
            launch {
                runCatching {
                    runtimeCoordinator.update(query, CommandAction.Type())
                }
            }

            combine(contentFlow, cachedResultsFlow) { liveResults, cachedResults ->
                val rankedLiveResults = searchResultRanker.rankLive(
                    query = query,
                    contentSnapshot = liveResults,
                    limit = limit
                )
                SearchResultSet(
                    results = searchResultRanker.merge(
                        liveResults = rankedLiveResults,
                        cachedResults = cachedResults,
                        limit = limit
                    ),
                )
            }.flowOn(Dispatchers.Default).collectLatest { searchResults ->
                send(searchResults)
            }
        }
    }
}
