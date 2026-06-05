package ru.raydroid.plugin.host.impl.data.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.IOException
import raydroid.plugin.host.impl.generated.resources.Res
import ru.raydroid.plugin.host.api.domain.model.PluginId

class ResourcePluginDataSourceImpl(
    private val json: Json,
) : ResourcePluginDataSource {
    override suspend fun load(pluginId: PluginId): ByteArray? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Res.readBytes("files/${pluginId.id}.rext")
            } catch (_: IOException) {
                null
            }
        }

    override suspend fun listPlugins(): List<PluginId> {
        val pluginListBytes = Res.readBytes("files/plugin-list.json")
        val pluginsIds = json.decodeFromString<List<String>>(pluginListBytes.decodeToString())
        return pluginsIds.map { plugin -> PluginId(plugin) }
    }
}
