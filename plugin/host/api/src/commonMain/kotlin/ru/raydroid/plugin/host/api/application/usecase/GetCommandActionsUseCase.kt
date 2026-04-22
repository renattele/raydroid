package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction

class GetCommandActionsUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry
) {
    suspend operator fun invoke(
        resultId: SearchResultId,
        target: CommandActionTarget
    ): List<PluginCommandListAction> {
        val runtime = pluginRuntimeRegistry.get()
            .runtimes()
            .value
            .firstOrNull { runtime -> runtime.pluginId == resultId.pluginId }
            ?: return emptyList()
        return runtime.actions(resultId.commandName, target)
    }
}
