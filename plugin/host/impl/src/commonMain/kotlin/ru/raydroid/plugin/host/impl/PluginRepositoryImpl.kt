package ru.raydroid.plugin.host.impl

import ru.raydroid.plugin.host.api.Plugin
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.api.PluginRepository
import ru.raydroid.plugin.host.api.exception.NonMatchingSignatureException
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.datasource.RemotePluginDataSource

class PluginRepositoryImpl(
    private val remotePluginDataSource: RemotePluginDataSource,
    private val localPluginDataSource: LocalPluginDataSource,
    private val pluginLoader: PluginLoader
) : PluginRepository {
    override suspend fun installPlugin(url: String) {
        val remotePlugin = remotePluginDataSource.load(url) ?: return
        val metadata = pluginLoader.loadPluginMetadata(Plugin(remotePlugin.data)) ?: return
        if (localPluginDataSource.signature(metadata.pluginId) != remotePlugin.signature) {
            throw NonMatchingSignatureException()
        }
        localPluginDataSource.load(metadata.pluginId)
    }

    override suspend fun listInstalledPlugins(): List<PluginId> {
        return localPluginDataSource.listPlugins()
    }

    override suspend fun loadPlugin(pluginId: PluginId): Plugin? {
        val data = localPluginDataSource.load(pluginId) ?: return null
        return Plugin(data)
    }

    override suspend fun deletePlugin(pluginId: PluginId) {
        localPluginDataSource.delete(pluginId)
    }
}