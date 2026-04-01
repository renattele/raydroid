package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class GetPluginsUseCase(
    private val pluginRuntimeRegistry: PluginRuntimeRegistry
) {
    operator fun invoke(): StateFlow<List<PluginRuntime>> {
        return pluginRuntimeRegistry.get().runtimes()
    }
}
