package ru.raydroid.plugin.host.api.domain.runtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation

interface PluginRuntimeCoordinator {
    fun cachedItems(): Flow<List<SearchIndexMutation>>

    fun runtimes(): StateFlow<List<PluginRuntime>>

    fun content(): StateFlow<List<ContentItem>>

    fun commands(): StateFlow<List<CommandItem>>

    suspend fun update(action: CommandActionBridge)

    data class ContentItem(
        val runtime: PluginRuntime,
        val presentation: PluginCommandPresentation,
        val listEntry: PluginCommandListItem,
        val resultId: SearchResultId,
    )

    data class CommandItem(
        val runtime: PluginRuntime,
        val listEntry: PluginCommandListItem,
        val resultId: SearchResultId,
    )
}
