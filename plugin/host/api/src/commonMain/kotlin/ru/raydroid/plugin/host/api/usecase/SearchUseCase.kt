package ru.raydroid.plugin.host.api.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SearchRepository
import ru.raydroid.plugin.host.api.SearchResults

class SearchUseCase(
    private val runtimeManager: PluginRuntimeManager,
    private val searchRepository: SearchRepository,
    private val coroutineScope: CoroutineScope
) {
    suspend operator fun invoke(query: String, limit: Int = 50): Flow<SearchResults> {
        val runtime = runtimeManager.get()
        coroutineScope.launch {
            runtime.update(query, CommandAction.Type())
        }
        val contentFlow = runtime.content()
        val cachedItemsFlow = searchRepository.search(query, limit)
        return combine(contentFlow, cachedItemsFlow) { content, cachedItems ->
            SearchResults(
                cachedResults = cachedItems,
                content = content
            )
        }
    }
}