package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.service.PluginLoader

class LoadRuntimesUseCase(
    private val pluginRepository: PluginRepository,
    private val pluginRuntimeRegistry: PluginRuntimeRegistry,
    private val pluginLoader: PluginLoader,
    private val hostPlatform: Platform,
) {
    suspend operator fun invoke() {
        val loadedPluginIds =
            pluginRuntimeRegistry
                .get()
                .runtimes()
                .value
                .map { runtime -> runtime.pluginId }
                .toSet()
        val installedPluginIds = pluginRepository.listInstalledPlugins()
        installedPluginIds.forEach { pluginId ->
            if (pluginId in loadedPluginIds) return@forEach
            val pluginArtifact = pluginRepository.loadPlugin(pluginId) ?: return@forEach
            val pluginMetadata = pluginLoader.loadPluginMetadata(pluginArtifact)
            if (!pluginMetadata.manifest.supports(hostPlatform)) return@forEach
            val pluginRuntime = pluginLoader.loadPlugin(pluginArtifact) ?: return@forEach
            pluginRuntimeRegistry.load(pluginRuntime)
        }
    }
}

private fun Manifest.supports(platform: Platform): Boolean = platforms.isEmpty() || platform in platforms
