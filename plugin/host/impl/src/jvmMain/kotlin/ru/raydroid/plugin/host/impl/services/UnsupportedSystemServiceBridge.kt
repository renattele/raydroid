package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.host.impl.DesktopPlatform

internal class UnsupportedSystemServiceBridge(
    private val platform: DesktopPlatform,
) : SystemServiceBridge {
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> = unsupported()

    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        unsupported()
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        unsupported()
    }

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("System bridge is not implemented for $platform desktop hosts")
}
