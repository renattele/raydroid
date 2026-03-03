package ru.raydroid.plugin.host.api.usecase

import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.api.PluginRepository
import ru.raydroid.plugin.host.api.PluginRuntimeManager

class LoadRuntimesUseCase(
    private val pluginRepository: PluginRepository,
    private val pluginRuntimeManager: PluginRuntimeManager,
    private val pluginLoader: PluginLoader
) {
    suspend operator fun invoke() {
        val installedPluginIds = pluginRepository.listInstalledPlugins()
        installedPluginIds.forEach { pluginId ->
            val plugin = pluginRepository.loadPlugin(pluginId) ?: return@forEach
            val pluginRuntime = pluginLoader.loadPlugin(plugin) ?: return@forEach
            pluginRuntimeManager.load(pluginRuntime)
        }
    }
}