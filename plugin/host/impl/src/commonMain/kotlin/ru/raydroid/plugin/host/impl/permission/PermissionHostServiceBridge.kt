package ru.raydroid.plugin.host.impl.permission

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

internal class PermissionHostServiceBridge(
    private val bridge: HostServiceBridge,
    private val manifest: Manifest
) : HostServiceBridge {
    override val cacheBridge: CacheServiceBridge =
        PermissionCacheServiceBridge(bridge.cacheBridge, manifest)
    override val clipboardBridge: ClipboardServiceBridge =
        PermissionClipboardServiceBridge(bridge.clipboardBridge, manifest)
    override val environmentBridge: EnvironmentServiceBridge =
        PermissionEnvironmentServiceBridge(bridge.environmentBridge, manifest)
    override val networkBridge: NetworkServiceBridge =
        PermissionNetworkServiceBridge(bridge.networkBridge, manifest)
    override val notificationBridge: NotificationServiceBridge =
        PermissionNotificationServiceBridge(bridge.notificationBridge, manifest)
    override val preferencesBridge: PreferencesServiceBridge =
        PermissionPreferencesServiceBridge(bridge.preferencesBridge, manifest)
    override val storageBridge: StorageServiceBridge =
        PermissionStorageServiceBridge(bridge.storageBridge, manifest)
    override val systemBridge: SystemServiceBridge = PermissionSystemServiceBridge(
        bridge.systemBridge, manifest
    )

}