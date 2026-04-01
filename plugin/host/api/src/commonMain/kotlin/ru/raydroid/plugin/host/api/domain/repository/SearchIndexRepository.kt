package ru.raydroid.plugin.host.api.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet

interface SearchIndexRepository {
    suspend fun update(mutations: List<SearchIndexMutation>)
    suspend fun updateUsage(resultId: SearchResultId)
    fun search(query: String, limit: Int = 50): Flow<List<SearchResultSet.SearchResult>>
}
