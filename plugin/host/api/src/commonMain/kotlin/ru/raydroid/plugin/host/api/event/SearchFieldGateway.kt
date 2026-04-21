package ru.raydroid.plugin.host.api.event

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.host.api.domain.model.PluginId

interface SearchFieldGateway {
    fun get(): Flow<SearchFieldRequest>
    suspend fun emit(request: SearchFieldRequest)
}

data class SearchFieldRequest(
    val pluginId: PluginId,
    val state: SearchFieldState
)
