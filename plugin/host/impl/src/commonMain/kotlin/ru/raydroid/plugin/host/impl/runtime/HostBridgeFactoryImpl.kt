package ru.raydroid.plugin.host.impl.runtime

import ru.raydroid.plugin.api.host.transport.CacheServiceBridge
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.api.host.transport.NetworkServiceBridge
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.transport.SearchFieldServiceBridge
import ru.raydroid.plugin.api.host.transport.StorageServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.service.HostBridgeFactory
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.impl.permission.PermissionHostServiceBridge
import ru.raydroid.plugin.host.impl.services.HostServiceBridgeImpl

class HostBridgeFactoryImpl(
    private val networkBridge: (PluginId) -> NetworkServiceBridge,
    private val notificationBridge: (PluginId) -> NotificationServiceBridge,
    private val preferencesBridge: (PluginId) -> PreferencesServiceBridge,
    private val searchFieldBridge: (PluginId) -> SearchFieldServiceBridge,
    private val cacheBridge: (PluginId) -> CacheServiceBridge,
    private val storageBridge: (PluginId) -> StorageServiceBridge,
    private val filesystemBridge: (PluginId) -> FileSystemServiceBridge,
    private val clipboardBridge: (PluginId) -> ClipboardServiceBridge,
    private val contactsBridge: (PluginId) -> ContactsServiceBridge,
    private val environmentBridge: (PluginId) -> EnvironmentServiceBridge,
    private val systemBridge: (PluginId) -> SystemServiceBridge,
) : HostBridgeFactory {
    override fun get(
        pluginId: PluginId,
        manifest: Manifest,
    ): HostServiceBridge {
        val bridge =
            HostServiceBridgeImpl(
                cacheBridge = cacheBridge(pluginId),
                clipboardBridge = clipboardBridge(pluginId),
                contactsBridge = contactsBridge(pluginId),
                environmentBridge = environmentBridge(pluginId),
                filesystemBridge = filesystemBridge(pluginId),
                networkBridge = networkBridge(pluginId),
                notificationBridge = notificationBridge(pluginId),
                preferencesBridge = preferencesBridge(pluginId),
                searchFieldBridge = searchFieldBridge(pluginId),
                storageBridge = storageBridge(pluginId),
                systemBridge = systemBridge(pluginId),
            )
        val permissionBridge = PermissionHostServiceBridge(bridge, manifest)
        return permissionBridge
    }
}
