package ru.raydroid.plugin.host.impl

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.host.bridge.CacheServiceBridge
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.bridge.HostServiceBridge
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.bridge.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import ru.raydroid.plugin.host.api.HostFactory
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.impl.permission.PermissionHostServiceBridge
import ru.raydroid.plugin.host.impl.services.HostServiceBridgeImpl

class HostFactoryImpl(
    private val networkBridge: (PluginId) -> NetworkServiceBridge,
    private val notificationBridge: (PluginId) -> NotificationServiceBridge,
    private val preferencesBridge: (PluginId) -> PreferencesServiceBridge,
    private val cacheBridge: (PluginId) -> CacheServiceBridge,
    private val storageBridge: (PluginId) -> StorageServiceBridge,
    private val clipboardBridge: (PluginId) -> ClipboardServiceBridge,
    private val environmentBridge: (PluginId) -> EnvironmentServiceBridge,
    private val systemBridge: (PluginId) -> SystemServiceBridge
): HostFactory {
    override fun get(pluginId: PluginId, manifest: Manifest): HostServiceBridge {
         val bridge = HostServiceBridgeImpl(
            cacheBridge = cacheBridge(pluginId),
            clipboardBridge = clipboardBridge(pluginId),
            environmentBridge = environmentBridge(pluginId),
            networkBridge = networkBridge(pluginId),
            notificationBridge = notificationBridge(pluginId),
            preferencesBridge = preferencesBridge(pluginId),
            storageBridge = storageBridge(pluginId),
            systemBridge = systemBridge(pluginId)
        )
        val permissionBridge = PermissionHostServiceBridge(bridge, manifest)
        return permissionBridge
    }
}