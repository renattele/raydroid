package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.service.EnvironmentService

internal class EnvironmentServiceImpl(
    private val bridge: EnvironmentServiceBridge
): EnvironmentService {
    override fun get(key: String): String? {
        return bridge.get(key)
    }

    override val platform: Platform = bridge.platform
}