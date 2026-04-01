package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet

class SearchUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val searchIndexRepository: SearchIndexRepository,
    private val coroutineScope: CoroutineScope
) {
    operator fun invoke(query: String, limit: Int = 50): Flow<SearchResultSet> {
        val runtimeCoordinator = pluginRuntimeRegistry.get()
        coroutineScope.launch {
            runtimeCoordinator.update(query, CommandAction.Type())
        }
        val contentFlow = runtimeCoordinator.content()
        val cachedResultsFlow = searchIndexRepository.search(query, limit)
        return combine(contentFlow, cachedResultsFlow) { liveResults, cachedResults ->
            SearchResultSet(
                results = mapLiveResults(liveResults) + cachedResults,
            )
        }
    }

    private fun mapLiveResults(
        contentSnapshot: List<PluginRuntimeCoordinator.ContentItem>
    ): List<SearchResultSet.LiveSearchResult> {
        return contentSnapshot.map { contentItem ->
            SearchResultSet.LiveSearchResult(
                listEntry = contentItem.listEntry,
                resultId = contentItem.resultId,
                runtime = contentItem.runtime,
                presentation = contentItem.presentation
            )
        }
    }
}
