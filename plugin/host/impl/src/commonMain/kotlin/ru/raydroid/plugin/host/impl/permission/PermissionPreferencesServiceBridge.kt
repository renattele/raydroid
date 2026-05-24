package ru.raydroid.plugin.host.impl.permission

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.readable
import ru.raydroid.plugin.api.manifest.writable

internal class PermissionPreferencesServiceBridge(
    private val preferencesServiceBridge: PreferencesServiceBridge,
    private val manifest: Manifest,
) : PreferencesServiceBridge {
    override fun get(key: String): Flow<String?> {
        if (manifest.access.preferences
                ?.permissions
                ?.readable() == true
        ) {
            return preferencesServiceBridge[key]
        }
        throw PermissionDenied()
    }

    override suspend fun set(
        key: String,
        value: String,
    ) {
        if (manifest.access.preferences
                ?.permissions
                ?.writable() == true
        ) {
            preferencesServiceBridge[key] = value
        }
        throw PermissionDenied()
    }
}
