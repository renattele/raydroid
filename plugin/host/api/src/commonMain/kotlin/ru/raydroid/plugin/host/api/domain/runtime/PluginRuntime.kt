package ru.raydroid.plugin.host.api.domain.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.api.ui.PluginRayNodeData

interface PluginRuntime {
    val pluginId: PluginId
    val manifest: Manifest
    val resources: FileSystem

    fun cachedItems(chunkSize: Int = 100): Flow<List<SearchIndexMutation>>

    suspend fun cachedItems(
        commandName: String,
        requestedItems: List<CommandItemId>,
        chunkSize: Int = 100,
    ): List<SearchIndexMutation>

    fun content(): StateFlow<List<ContentItem>>

    fun fullscreen(commandName: String): StateFlow<FullscreenContent?>

    suspend fun actions(
        commandName: String,
        itemId: CommandItemId,
    ): List<PluginCommandListAction>

    suspend fun update(action: CommandActionBridge)

    suspend fun update(
        commandName: String,
        action: CommandActionBridge,
    )

    suspend fun back(commandName: String): Boolean

    suspend fun unload()

    data class CommandCacheRequest(
        val commandName: String,
        val requestedItems: List<CommandItemId>,
        val chunkSize: Int,
    )

    data class ContentItem(
        val commandName: String,
        val presentation: PluginCommandPresentation,
    )

    data class FullscreenContent(
        val commandName: String,
        val content: List<PluginRayNodeData>,
    )
}
