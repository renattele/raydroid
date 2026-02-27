package ru.raydroid.plugin.host.impl.datasource

import ru.raydroid.plugin.host.api.PluginId

interface ResourcePluginDataSource {
    suspend fun load(pluginId: PluginId): ByteArray?
    suspend fun listPlugins(): List<PluginId>
}