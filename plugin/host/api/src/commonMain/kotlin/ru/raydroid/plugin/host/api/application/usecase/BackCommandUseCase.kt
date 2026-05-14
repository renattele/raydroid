package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class BackCommandUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry
) {
    suspend operator fun invoke(resultId: SearchResultId): Boolean {
        if (resultId.itemId != CommandItemId.CommandRoot) return false
        val runtime = pluginRuntimeRegistry.get().runtimes().value.firstOrNull { runtime ->
            runtime.pluginId == resultId.pluginId
        } ?: return false
        return runtime.back(resultId.commandName)
    }
}
