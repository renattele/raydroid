package ru.raydroid.plugin.host.api.usecase

import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SinglePluginRuntime

class GetPluginsUseCase(
    private val pluginRuntimeManager: PluginRuntimeManager
) {
    operator fun invoke(): StateFlow<List<SinglePluginRuntime>> {
        return pluginRuntimeManager.get().runtimes()
    }
}