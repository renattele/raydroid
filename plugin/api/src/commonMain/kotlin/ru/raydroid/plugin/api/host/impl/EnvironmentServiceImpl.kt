package ru.raydroid.plugin.api.host.impl

import ru.raydroid.plugin.api.core.Platform
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.service.EnvironmentService

internal class EnvironmentServiceImpl(
    private val bridge: EnvironmentServiceBridge
): EnvironmentService {
    override fun get(key: String): String? {
        return bridge.get(key)
    }

    override val platform: Platform = bridge.platform
}