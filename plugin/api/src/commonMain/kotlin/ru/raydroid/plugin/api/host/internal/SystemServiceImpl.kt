package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.host.service.SystemService

internal class SystemServiceImpl(
    private val bridge: SystemServiceBridge
): SystemService {
    override suspend fun getApps(): List<SystemService.Application> {
        return bridge.getApps().map { it.toServiceApp() }
    }

    override suspend fun openApp(
        appId: String,
        options: SystemService.OpenOptions
    ) {
        bridge.openApp(appId, options.toBridgeOptions())
    }

    override suspend fun open(
        target: String,
        options: SystemService.OpenOptions
    ) {
        bridge.open(target, options.toBridgeOptions())
    }

    private fun SystemServiceBridge.RawApplication.toServiceApp() = SystemService.Application(
        name = name,
        id = id,
        icon = icon
    )

    private fun SystemService.Application.toBridgeApp() = SystemServiceBridge.RawApplication(
        name = name,
        id = id,
        icon = icon
    )

    private fun SystemServiceBridge.OpenOptions.toServiceOptions() = SystemService.OpenOptions(
        keys = keys
    )

    private fun SystemService.OpenOptions.toBridgeOptions() = SystemServiceBridge.OpenOptions(
        keys = keys
    )
}