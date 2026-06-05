package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class CommandActionDispatcher(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
) {
    suspend fun dispatch(
        resultId: SearchResultId,
        action: CommandActionBridge,
    ) {
        val runtimes = pluginRuntimeRegistry.get().runtimes().value
        runtimes
            .filter { runtime -> runtime.pluginId == resultId.pluginId }
            .forEach { runtime -> runtime.update(resultId.commandName, action) }
    }
}
