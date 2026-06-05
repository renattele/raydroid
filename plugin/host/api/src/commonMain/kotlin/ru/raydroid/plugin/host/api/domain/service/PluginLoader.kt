package ru.raydroid.plugin.host.api.domain.service

import kotlinx.coroutines.flow.StateFlow
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginDescriptor
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator

interface PluginLoader {
    suspend fun loadPlugin(plugin: PluginArtifact): PluginRuntime?

    suspend fun loadPluginMetadata(plugin: PluginArtifact): PluginDescriptor

    suspend fun join(plugins: StateFlow<List<PluginRuntime>>): PluginRuntimeCoordinator
}
