package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun update(updateList: List<ListItemUpdate>)
    suspend fun updateUsage(listItemId: ListItemId)
    fun search(query: String, limit: Int = 50): Flow<List<SearchResults.Item>>
}
