package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.bridge.CacheServiceBridge
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.bridge.HostServiceBridge
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.bridge.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge

internal class HostServiceBridgeImpl(
    override val cacheBridge: CacheServiceBridge,
    override val clipboardBridge: ClipboardServiceBridge,
    override val environmentBridge: EnvironmentServiceBridge,
    override val networkBridge: NetworkServiceBridge,
    override val notificationBridge: NotificationServiceBridge,
    override val preferencesBridge: PreferencesServiceBridge,
    override val storageBridge: StorageServiceBridge,
    override val systemBridge: SystemServiceBridge
) : HostServiceBridge