package ru.raydroid.plugin.host.api.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId

interface SearchAliasRepository {
    fun observeAliases(): Flow<Map<SearchResultId, String>>
    suspend fun saveAlias(resultId: SearchResultId, alias: String): SearchAliasSaveResult
    suspend fun removeAlias(resultId: SearchResultId)
    suspend fun resolveExactAlias(alias: String): SearchResultId?
}
