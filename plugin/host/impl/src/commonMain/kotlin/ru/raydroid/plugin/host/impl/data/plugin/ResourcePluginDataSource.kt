package ru.raydroid.plugin.host.impl.data.plugin

import ru.raydroid.plugin.host.api.domain.model.PluginId

interface ResourcePluginDataSource {
    suspend fun load(pluginId: PluginId): ByteArray?

    suspend fun listPlugins(): List<PluginId>
}
