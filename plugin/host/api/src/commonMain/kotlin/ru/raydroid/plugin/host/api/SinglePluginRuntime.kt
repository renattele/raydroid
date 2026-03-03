package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems

interface SinglePluginRuntime {
    val manifest: Manifest
    val resources: FileSystem

    fun cachedItems(
        chunkSize: Int = 100
    ): Flow<List<ListItemUpdate>>

    fun content(): StateFlow<List<RayItems>>

    suspend fun update(query: String, action: CommandAction)
    suspend fun unload()
}
