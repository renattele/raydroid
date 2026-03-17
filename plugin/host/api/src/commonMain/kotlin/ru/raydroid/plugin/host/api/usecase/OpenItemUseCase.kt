package ru.raydroid.plugin.host.api.usecase

import ru.raydroid.plugin.api.core.CommandAction
import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SearchRepository

class OpenItemUseCase(
    private val runtimeManager: PluginRuntimeManager,
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(query: String, listItemId: ListItemId) {
        val runtimes = runtimeManager.get().runtimes().value
        runtimes
            .filter {
                it.pluginId == listItemId.pluginId
            }.forEach { runtime ->
                runtime.update(query, CommandAction.Enter(listItemId.itemId))
            }
        searchRepository.updateUsage(listItemId)
    }
}