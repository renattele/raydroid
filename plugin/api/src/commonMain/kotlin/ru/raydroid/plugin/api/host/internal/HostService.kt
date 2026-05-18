package ru.raydroid.plugin.api.host.internal

import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.api.host.service.HostService

internal class HostServiceImpl(
    bridge: HostServiceBridge,
    serializer: Json = Json
): HostService {
    override val cache = CacheServiceImpl(bridge.cacheBridge, serializer)
    override val clipboard = ClipboardServiceImpl(bridge.clipboardBridge)
    override val contacts = ContactsServiceImpl(bridge.contactsBridge)
    override val environment = EnvironmentServiceImpl(bridge.environmentBridge)
    override val filesystem = FileSystemServiceImpl(bridge.filesystemBridge)
    override val network = NetworkServiceImpl(bridge.networkBridge, serializer)
    override val notification = NotificationServiceImpl(bridge.notificationBridge)
    override val preferences = PreferencesServiceImpl(bridge.preferencesBridge, serializer)
    override val searchField = SearchFieldServiceImpl(bridge.searchFieldBridge)
    override val storage = StorageServiceImpl(bridge.storageBridge, serializer)
    override val system = SystemServiceImpl(bridge.systemBridge)
}
