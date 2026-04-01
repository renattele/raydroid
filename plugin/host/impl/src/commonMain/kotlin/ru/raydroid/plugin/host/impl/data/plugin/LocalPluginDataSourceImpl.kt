package ru.raydroid.plugin.host.impl.data.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import ru.raydroid.plugin.host.api.domain.model.PluginId

class LocalPluginDataSourceImpl(
    private val localFs: FileSystem,
    private val basePath: Path
) : LocalPluginDataSource {
    override suspend fun add(
        pluginId: PluginId,
        data: ByteArray
    ): Unit = withContext(Dispatchers.IO) {
        localFs.write(getPluginPath(pluginId)) {
            write(data)
        }
    }

    override suspend fun load(pluginId: PluginId): ByteArray? = withContext(Dispatchers.IO) {
        if (!pluginExists(pluginId)) return@withContext null
        localFs.read(getPluginPath(pluginId)) {
            readByteArray()
        }
    }

    override suspend fun hash(pluginId: PluginId): String? = withContext(Dispatchers.IO) {
        if (!pluginExists(pluginId)) return@withContext null
        val data = localFs.read(getPluginPath(pluginId)) {
            readByteString()
        }
        return@withContext data.sha256().hex()
    }

    override suspend fun signature(pluginId: PluginId): String? {
        // TODO: implement
        return null
    }

    override suspend fun listPlugins(): List<PluginId> = withContext(Dispatchers.IO) {
        localFs.list(basePath).filter { path ->
            path.name.endsWith(".rext")
        }.map { path ->
            PluginId(path.name.drop(5))
        }
    }

    override suspend fun delete(pluginId: PluginId) = withContext(Dispatchers.IO) {
        localFs.delete(getPluginPath(pluginId))
    }

    private fun pluginExists(pluginId: PluginId): Boolean {
        return localFs.exists(getPluginPath(pluginId))
    }

    private fun getPluginPath(
        pluginId: PluginId
    ): Path = basePath / (pluginId.id + ".rext")
}