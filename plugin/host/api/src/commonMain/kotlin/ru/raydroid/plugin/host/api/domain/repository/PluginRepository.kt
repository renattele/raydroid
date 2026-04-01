package ru.raydroid.plugin.host.api.domain.repository

import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginId

interface PluginRepository {
    suspend fun installPlugin(url: String)
    suspend fun listInstalledPlugins(): List<PluginId>
    suspend fun loadPlugin(pluginId: PluginId): PluginArtifact?
    suspend fun deletePlugin(pluginId: PluginId)
}
