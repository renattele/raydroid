package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository

class OpenItemUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val searchIndexRepository: SearchIndexRepository
) {
    suspend operator fun invoke(query: String, resultId: SearchResultId) {
        val runtimes = pluginRuntimeRegistry.get().runtimes().value
        runtimes
            .filter {
                it.pluginId == resultId.pluginId
            }.forEach { runtime ->
                runtime.update(query, CommandAction.Enter(resultId.itemId))
            }
        searchIndexRepository.updateUsage(resultId)
    }
}
