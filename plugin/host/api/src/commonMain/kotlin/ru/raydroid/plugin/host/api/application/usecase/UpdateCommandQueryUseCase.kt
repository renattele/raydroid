package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class UpdateCommandQueryUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry
) {
    suspend operator fun invoke(resultId: SearchResultId, query: String) {
        val runtimes = pluginRuntimeRegistry.get().runtimes().value
        runtimes
            .filter { runtime -> runtime.pluginId == resultId.pluginId }
            .forEach { runtime ->
                runtime.update(
                    commandName = resultId.commandName,
                    query = query,
                    action = CommandAction.Type()
                )
            }
    }
}
