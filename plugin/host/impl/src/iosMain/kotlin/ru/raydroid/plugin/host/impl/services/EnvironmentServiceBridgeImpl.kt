package ru.raydroid.plugin.host.impl.services

import platform.Foundation.NSProcessInfo
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.manifest.Platform

internal class EnvironmentServiceBridgeImpl : EnvironmentServiceBridge {
    override fun get(key: String): String? =
        NSProcessInfo.processInfo.environment[key] as? String

    override val platform: Platform = Platform.IOS
}
