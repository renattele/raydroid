package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.transport.CacheServiceBridge
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.api.host.transport.NetworkServiceBridge
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.transport.StorageServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge

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