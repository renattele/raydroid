package ru.raydroid.plugin.host.impl.data.plugin

import ru.raydroid.plugin.host.api.domain.model.PluginId

interface LocalPluginDataSource {
    suspend fun add(pluginId: PluginId, data: ByteArray)
    suspend fun load(pluginId: PluginId): ByteArray?
    suspend fun hash(pluginId: PluginId): String?
    suspend fun signature(pluginId: PluginId): String?
    suspend fun listPlugins(): List<PluginId>
    suspend fun delete(pluginId: PluginId)
}