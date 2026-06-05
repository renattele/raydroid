package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.CacheServiceBridge
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import ru.raydroid.plugin.api.host.transport.NetworkServiceBridge
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge
import ru.raydroid.plugin.api.host.transport.SearchFieldServiceBridge
import ru.raydroid.plugin.api.host.transport.StorageServiceBridge
import ru.raydroid.plugin.host.api.event.NotificationEvent

internal expect val platformHostServiceModule: Module
internal val hostServiceModule =
    module {
        includes(platformHostServiceModule)
        factory<NetworkServiceBridge> { NetworkServiceBridgeImpl(get()) }
        factory<NotificationServiceBridge> { params ->
            NotificationServiceBridgeImpl(eventGateway = get(), pluginId = params.get())
        }

        factory<PreferencesServiceBridge> { PreferencesServiceBridgeImpl(get()) }
        factory<SearchFieldServiceBridge> { params ->
            SearchFieldServiceBridgeImpl(
                pluginId = params.get(),
                searchFieldGateway = get(),
            )
        }
        factory<CacheServiceBridge> { RuntimeCacheServiceImpl() }
        factory<FileSystemServiceBridge> {
            FileSystemServiceBridgeImpl(
                fileSystem = get(named("localFileSystem")),
                platformFileSystemGateway = get(),
            )
        }
        factory<StorageServiceBridge> {
            StorageServiceBridgeImpl(
                basePath = get(named("localPath")),
                filePrefix = "plugin",
            )
        }
    }
