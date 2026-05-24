package ru.raydroid.plugin.host.impl.permission

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.CacheServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Permission
import ru.raydroid.plugin.api.manifest.readable
import ru.raydroid.plugin.api.manifest.writable

internal class PermissionCacheServiceBridge(
    private val cacheServiceBridge: CacheServiceBridge,
    private val manifest: Manifest,
) : CacheServiceBridge {
    override suspend fun get(key: String): String? {
        if (manifest.access.cache
                ?.permissions
                ?.readable() == true
        ) {
            return cacheServiceBridge[key]
        }
        throw PermissionDenied()
    }

    override suspend fun set(
        key: String,
        value: String,
    ) {
        if (manifest.access.cache
                ?.permissions
                ?.writable() == true
        ) {
            cacheServiceBridge[key] = value
        }
        throw PermissionDenied()
    }

    override suspend fun clear() {
        if (manifest.access.cache
                ?.permissions
                ?.contains(Permission.Manage) == true
        ) {
            cacheServiceBridge.clear()
        }
    }

    override suspend fun flowOf(key: String): Flow<String?> {
        if (manifest.access.cache
                ?.permissions
                ?.contains(Permission.Manage) == true
        ) {
            return cacheServiceBridge.flowOf(key)
        }
        throw PermissionDenied()
    }
}
