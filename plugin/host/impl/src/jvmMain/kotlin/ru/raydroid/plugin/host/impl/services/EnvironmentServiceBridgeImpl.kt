package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.host.impl.detectDesktopPlatform
import ru.raydroid.plugin.host.impl.toManifestPlatform

internal class EnvironmentServiceBridgeImpl(
    private val environmentLookup: (String) -> String? = System::getenv,
    override val platform: Platform = detectDesktopPlatform().toManifestPlatform(),
) : EnvironmentServiceBridge {
    override fun get(key: String): String? = environmentLookup(key)
}
