package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.manifest.readable

internal class PermissionEnvironmentServiceBridge(
    private val environmentServiceBridge: EnvironmentServiceBridge,
    private val manifest: Manifest,
) : EnvironmentServiceBridge {
    override fun get(key: String): String? {
        if (manifest.access.environment
                ?.permissions
                ?.readable() == true
        ) {
            if (manifest.access.environment
                    ?.access
                    ?.let { hasAccess(it, key) } == true
            ) {
                return environmentServiceBridge.get(key)
            }
        }
        throw PermissionDenied()
    }

    private fun hasAccess(
        accessList: List<String>,
        key: String,
    ): Boolean =
        accessList.any { accessString ->
            accessString.toRegex().matches(key)
        }

    override val platform: Platform
        get() = environmentServiceBridge.platform
}
