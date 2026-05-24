package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.host.service.EnvironmentService
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.manifest.Platform

internal class EnvironmentServiceImpl(
    private val bridge: EnvironmentServiceBridge,
) : EnvironmentService {
    override fun get(key: String): String? = bridge.get(key)

    override val platform: Platform = bridge.platform
}
