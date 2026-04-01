package ru.raydroid.plugin.host.api.domain.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.presentation.CommandPresentation

interface PluginRuntime {
    val pluginId: PluginId
    val manifest: Manifest
    val resources: FileSystem

    fun cachedItems(
        chunkSize: Int = 100
    ): Flow<List<SearchIndexMutation>>

    fun content(): StateFlow<List<ContentItem>>

    suspend fun update(query: String, action: CommandAction)
    suspend fun unload()

    data class ContentItem(
        val commandName: String,
        val presentation: CommandPresentation
    )
}
