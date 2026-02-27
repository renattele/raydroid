package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.core.Platform
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge

internal class EnvironmentServiceBridgeImpl: EnvironmentServiceBridge {
    override fun get(key: String): String? {
        // TODO: Implement
        return null
    }

    override val platform: Platform = Platform.Android
}