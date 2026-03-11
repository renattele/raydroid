package ru.raydroid.plugin.host.impl.permission

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.readable
import ru.raydroid.plugin.api.core.writable
import ru.raydroid.plugin.api.host.bridge.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionPreferencesServiceBridge(
    private val preferencesServiceBridge: PreferencesServiceBridge,
    private val manifest: Manifest
): PreferencesServiceBridge {
    override fun get(key: String): Flow<String?> {
        if (manifest.access.preferences?.permissions?.readable() == true) {
            return preferencesServiceBridge[key]
        }
        throw PermissionDenied()
    }

    override suspend fun set(key: String, value: String) {
        if (manifest.access.preferences?.permissions?.writable() == true) {
            preferencesServiceBridge[key] = value
        }
        throw PermissionDenied()
    }
}