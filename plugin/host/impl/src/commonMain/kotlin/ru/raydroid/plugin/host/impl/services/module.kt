package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.bridge.CacheServiceBridge
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.bridge.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge
import ru.raydroid.plugin.host.api.NotificationEvent

internal expect val platformHostServiceModule: Module
internal val hostServiceModule = module {
    includes(platformHostServiceModule)
    factory<NetworkServiceBridge> { NetworkServiceBridgeImpl(get()) }
    factory<NotificationServiceBridge> { params ->
        NotificationServiceBridgeImpl(eventGateway = get(), pluginId = params.get())
    }

    factory<PreferencesServiceBridge> { PreferencesServiceBridgeImpl(get()) }
    factory<CacheServiceBridge> { RuntimeCacheServiceImpl() }
    factory<StorageServiceBridge> {
        StorageServiceBridgeImpl(
            basePath = get(named("localPath")),
            filePrefix = "plugin"
        )
    }
}