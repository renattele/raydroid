package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class LoadRuntimesUseCase(
    private val pluginRepository: PluginRepository,
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val pluginLoader: PluginLoader
) {
    suspend operator fun invoke() {
        val loadedPluginIds = pluginRuntimeRegistry.get()
            .runtimes()
            .value
            .map { runtime -> runtime.pluginId }
            .toSet()
        val installedPluginIds = pluginRepository.listInstalledPlugins()
        installedPluginIds.forEach { pluginId ->
            if (pluginId in loadedPluginIds) return@forEach
            val pluginArtifact = pluginRepository.loadPlugin(pluginId) ?: return@forEach
            val pluginRuntime = pluginLoader.loadPlugin(pluginArtifact) ?: return@forEach
            pluginRuntimeRegistry.load(pluginRuntime)
        }
    }
}
