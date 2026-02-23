package ru.raydroid.plugin.api.host.impl

import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import ru.raydroid.plugin.api.host.service.SystemService

internal class SystemServiceImpl(
    private val bridge: SystemServiceBridge
): SystemService {
    override suspend fun getApps(): List<SystemService.Application> {
        return bridge.getApps().map { it.toServiceApp() }
    }

    override suspend fun open(
        app: SystemService.Application,
        options: SystemService.OpenOptions
    ) {
        bridge.open(app.toBridgeApp(), options.toBridgeOptions())
    }

    override suspend fun open(
        target: String,
        options: SystemService.OpenOptions
    ) {
        bridge.open(target, options.toBridgeOptions())
    }

    private fun SystemServiceBridge.RawApplication.toServiceApp() = SystemService.Application(
        name = name,
        id = id
    )

    private fun SystemService.Application.toBridgeApp() = SystemServiceBridge.RawApplication(
        name = name,
        id = id
    )

    private fun SystemServiceBridge.OpenOptions.toServiceOptions() = SystemService.OpenOptions(
        keys = keys
    )

    private fun SystemService.OpenOptions.toBridgeOptions() = SystemServiceBridge.OpenOptions(
        keys = keys
    )
}