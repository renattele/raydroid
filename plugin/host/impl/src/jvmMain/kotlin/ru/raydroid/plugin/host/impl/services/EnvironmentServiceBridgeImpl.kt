package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.manifest.Platform

internal class EnvironmentServiceBridgeImpl : EnvironmentServiceBridge {
    override fun get(key: String): String? {
        // TODO: Implement
        return null
    }

    override val platform: Platform = Platform.MacOS
}
