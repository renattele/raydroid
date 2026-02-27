package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge

internal class SystemServiceBridgeImpl: SystemServiceBridge {
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        TODO("Not yet implemented")
    }

    override suspend fun open(
        app: SystemServiceBridge.RawApplication,
        options: SystemServiceBridge.OpenOptions
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        TODO("Not yet implemented")
    }

}