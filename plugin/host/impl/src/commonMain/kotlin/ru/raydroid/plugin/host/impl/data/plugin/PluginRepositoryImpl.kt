package ru.raydroid.plugin.host.impl.data.plugin

import ru.raydroid.plugin.host.api.domain.exception.NonMatchingSignatureException
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.impl.data.plugin.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.RemotePluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.ResourcePluginDataSource

class PluginRepositoryImpl(
    private val remotePluginDataSource: RemotePluginDataSource,
    private val localPluginDataSource: LocalPluginDataSource,
    private val resourcePluginDataSource: ResourcePluginDataSource,
    private val pluginLoader: PluginLoader,
) : PluginRepository {
    override suspend fun installPlugin(url: String) {
        val remotePlugin = remotePluginDataSource.load(url) ?: return
        val metadata =
            pluginLoader.loadPluginMetadata(
                PluginArtifact(
                    data = remotePlugin.data,
                    signature = remotePlugin.signature,
                ),
            )
        val localSignature = localPluginDataSource.signature(metadata.pluginId)
        if (localSignature != null && remotePlugin.signature != null && localSignature != remotePlugin.signature) {
            throw NonMatchingSignatureException()
        }
        localPluginDataSource.add(
            pluginId = metadata.pluginId,
            data = remotePlugin.data,
            signature = remotePlugin.signature,
        )
    }

    override suspend fun listInstalledPlugins(): List<PluginId> =
        resourcePluginDataSource.listPlugins() + localPluginDataSource.listPlugins()

    override suspend fun loadPlugin(pluginId: PluginId): PluginArtifact? {
        val resourceData = resourcePluginDataSource.load(pluginId)
        if (resourceData != null) {
            return PluginArtifact(resourceData)
        }
        val localData = localPluginDataSource.load(pluginId) ?: return null

        return PluginArtifact(
            data = localData,
            signature = localPluginDataSource.signature(pluginId),
        )
    }

    override suspend fun deletePlugin(pluginId: PluginId) {
        localPluginDataSource.delete(pluginId)
    }
}
