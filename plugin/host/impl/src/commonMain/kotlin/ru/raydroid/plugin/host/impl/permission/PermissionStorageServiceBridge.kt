package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.readable
import ru.raydroid.plugin.api.core.writable
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionStorageServiceBridge(
    private val storageServiceBridge: StorageServiceBridge,
    private val manifest: Manifest
): StorageServiceBridge {
    override suspend fun get(key: String): String? {
        if (manifest.access.storage?.permissions?.readable() == true) {
            return storageServiceBridge[key]
        }
        throw PermissionDenied()
    }

    override suspend fun set(key: String, value: String) {
        if (manifest.access.storage?.permissions?.writable() == true) {
            return storageServiceBridge.set(key, value)
        }
        throw PermissionDenied()
    }

    override suspend fun has(key: String): Boolean {
        if (manifest.access.storage?.permissions?.readable() == true) {
            return storageServiceBridge.has(key)
        }
        throw PermissionDenied()
    }
}