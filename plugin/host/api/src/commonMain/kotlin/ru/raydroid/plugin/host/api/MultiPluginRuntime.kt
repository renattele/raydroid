package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.RayItems

interface MultiPluginRuntime {
    fun cachedItems(): Flow<Map<SinglePluginRuntime, List<ListItemUpdate>>>

    fun runtimes(): StateFlow<List<SinglePluginRuntime>>

    fun content(): StateFlow<Map<SinglePluginRuntime, List<RayItems>>>

    suspend fun update(query: String, action: CommandAction)
}
