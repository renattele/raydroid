package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge

internal class StorageServiceBridgeImpl(
    private val basePath: Path,
    private val filePrefix: String
): StorageServiceBridge {
    override suspend fun get(key: String): String? {
        return withContext(Dispatchers.IO) {
            if (!has(key)) {
                return@withContext null
            }
            FileSystem.SYSTEM.read(getFilePath(key)) {
                readByteArray().decodeToString()
            }
        }
    }

    override suspend fun set(key: String, value: String) {
        withContext(Dispatchers.IO) {
            FileSystem.SYSTEM.write(getFilePath(key)) {
                write(value.encodeToByteArray())
            }
        }
    }

    override suspend fun has(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            FileSystem.SYSTEM.exists(getFilePath(key))
        }
    }

    private fun getFilePath(key: String): Path {
        return basePath / (filePrefix + key)
    }
}