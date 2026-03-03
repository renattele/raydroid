package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.core.ListItem

interface SearchRepository {
    suspend fun update(updateList: List<ListItemUpdate>)
    fun search(query: String, limit: Int = 50): Flow<List<ListItem>>
}