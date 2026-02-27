package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.RayItems

interface MultiPluginRuntime {
    suspend fun cachedItems(): List<ListItem>

    fun content(): StateFlow<List<List<RayItems>>>

    suspend fun update(query: String, action: CommandAction)
}