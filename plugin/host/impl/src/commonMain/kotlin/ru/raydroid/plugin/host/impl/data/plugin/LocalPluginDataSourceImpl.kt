package ru.raydroid.plugin.host.impl.data.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import ru.raydroid.plugin.host.api.domain.model.PluginId

class LocalPluginDataSourceImpl(
    private val localFs: FileSystem,
    private val basePath: Path,
) : LocalPluginDataSource {
    override suspend fun add(
        pluginId: PluginId,
        data: ByteArray,
        signature: String?,
    ): Unit =
        withContext(Dispatchers.IO) {
            localFs.write(getPluginPath(pluginId)) {
                write(data)
            }
            val signaturePath = getSignaturePath(pluginId)
            if (signature == null) {
                localFs.delete(signaturePath, mustExist = false)
            } else {
                localFs.write(signaturePath) {
                    writeUtf8(signature)
                }
            }
        }

    override suspend fun load(pluginId: PluginId): ByteArray? =
        withContext(Dispatchers.IO) {
            if (!pluginExists(pluginId)) return@withContext null
            localFs.read(getPluginPath(pluginId)) {
                readByteArray()
            }
        }

    override suspend fun hash(pluginId: PluginId): String? =
        withContext(Dispatchers.IO) {
            if (!pluginExists(pluginId)) return@withContext null
            val data =
                localFs.read(getPluginPath(pluginId)) {
                    readByteString()
                }
            return@withContext data.sha256().hex()
        }

    override suspend fun signature(pluginId: PluginId): String? {
        return withContext(Dispatchers.IO) {
            val signaturePath = getSignaturePath(pluginId)
            if (!localFs.exists(signaturePath)) return@withContext null
            localFs.read(signaturePath) {
                readUtf8()
            }
        }
    }

    override suspend fun listPlugins(): List<PluginId> =
        withContext(Dispatchers.IO) {
            localFs
                .list(basePath)
                .filter { path ->
                    path.name.endsWith(".rext")
                }.map { path ->
                    PluginId(path.name.drop(5))
                }
        }

    override suspend fun delete(pluginId: PluginId) =
        withContext(Dispatchers.IO) {
            localFs.delete(getPluginPath(pluginId))
            localFs.delete(getSignaturePath(pluginId), mustExist = false)
        }

    private fun pluginExists(pluginId: PluginId): Boolean = localFs.exists(getPluginPath(pluginId))

    private fun getPluginPath(pluginId: PluginId): Path = basePath / (pluginId.id + ".rext")

    private fun getSignaturePath(pluginId: PluginId): Path = basePath / (pluginId.id + ".sig")
}
