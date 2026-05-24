package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class GetCommandFullscreenUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
) {
    operator fun invoke(resultId: SearchResultId): Flow<PluginRuntime.FullscreenContent?> {
        if (resultId.itemId != CommandItemId.CommandRoot) return emptyFlow()
        val runtime =
            pluginRuntimeRegistry
                .get()
                .runtimes()
                .value
                .firstOrNull { runtime -> runtime.pluginId == resultId.pluginId }
                ?: return emptyFlow()
        return runtime.fullscreen(resultId.commandName)
    }
}
