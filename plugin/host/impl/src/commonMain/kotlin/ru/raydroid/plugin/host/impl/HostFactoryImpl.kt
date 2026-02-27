package ru.raydroid.plugin.host.impl

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
import ru.raydroid.plugin.host.impl.services.HostServiceBridgeImpl

class HostFactoryImpl(
    private val networkBridge: () -> NetworkServiceBridge,
    private val notificationBridge: () -> NotificationServiceBridge,
    private val preferencesBridge: () -> PreferencesServiceBridge,
    private val cacheBridge: () -> CacheServiceBridge,
    private val storageBridge: () -> StorageServiceBridge,
    private val clipboardBridge: () -> ClipboardServiceBridge,
    private val environmentBridge: () -> EnvironmentServiceBridge,
    private val systemBridge: () -> SystemServiceBridge
): HostFactory {
    override fun get(pluginId: PluginId): HostServiceBridge {
        return HostServiceBridgeImpl(
            cacheBridge = cacheBridge(),
            clipboardBridge = clipboardBridge(),
            environmentBridge = environmentBridge(),
            networkBridge = networkBridge(),
            notificationBridge = notificationBridge(),
            preferencesBridge = preferencesBridge(),
            storageBridge = storageBridge(),
            systemBridge = systemBridge()
        )
    }
}