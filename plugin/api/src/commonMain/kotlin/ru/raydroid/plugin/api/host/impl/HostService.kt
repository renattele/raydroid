package ru.raydroid.plugin.api.host.impl

import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.bridge.HostServiceBridge
import ru.raydroid.plugin.api.host.service.HostService

internal class HostServiceImpl(
    bridge: HostServiceBridge,
    serializer: Json = Json
): HostService {
    override val cache = CacheServiceImpl(bridge.cacheBridge, serializer)
    override val clipboard = ClipboardServiceImpl(bridge.clipboardBridge)
    override val environment = EnvironmentServiceImpl(bridge.environmentBridge)
    override val network = NetworkServiceImpl(bridge.networkBridge, serializer)
    override val notification = NotificationServiceImpl(bridge.notificationBridge)
    override val preferences = PreferencesServiceImpl(bridge.preferencesBridge, serializer)
    override val storage = StorageServiceImpl(bridge.storageBridge, serializer)
    override val system = SystemServiceImpl(bridge.systemBridge)
}