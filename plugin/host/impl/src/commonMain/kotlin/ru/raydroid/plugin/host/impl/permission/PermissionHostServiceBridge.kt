package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.host.transport.CacheServiceBridge
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.api.host.transport.NetworkServiceBridge
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.transport.StorageServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge

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