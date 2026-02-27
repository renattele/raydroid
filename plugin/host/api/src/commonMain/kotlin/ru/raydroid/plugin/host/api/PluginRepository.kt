package ru.raydroid.plugin.host.api

interface PluginRepository {
    suspend fun installPlugin(url: String)
    suspend fun listInstalledPlugins(): List<PluginId>
    suspend fun loadPlugin(pluginId: PluginId): Plugin?
    suspend fun deletePlugin(pluginId: PluginId)
}