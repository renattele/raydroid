package ru.raydroid.plugin.host.impl

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.raydroid.plugin.host.api.HostFactory
import ru.raydroid.plugin.host.api.PluginLoader
import ru.raydroid.plugin.host.api.PluginRepository
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSourceImpl
import ru.raydroid.plugin.host.impl.datasource.RemotePluginDataSource
import ru.raydroid.plugin.host.impl.datasource.RemotePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.datasource.ResourcePluginDataSource
import ru.raydroid.plugin.host.impl.datasource.ResourcePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.services.hostServiceModule

internal expect val pluginPlatformModule: Module
val pluginHostModule = module {
    includes(pluginPlatformModule)
    includes(hostServiceModule)
    single<PluginLoader> {
        PluginLoaderImpl({
            get()
        }, get(), get(), get())
    }
    single<LocalPluginDataSource> {
        LocalPluginDataSourceImpl(
            localFs = get(named("localFileSystem")),
            basePath = get(named("localPath"))
        )
    }
    single<ResourcePluginDataSource> {
        ResourcePluginDataSourceImpl(get())
    }
    singleOf(::RemotePluginDataSourceImpl) {
        bind<RemotePluginDataSource>()
    }
    singleOf(::PluginRepositoryImpl) bind PluginRepository::class
    single<HostFactory> {
        HostFactoryImpl(
            networkBridge = { get() },
            notificationBridge = { get() },
            preferencesBridge = { get() },
            cacheBridge = { get() },
            storageBridge = { get() },
            clipboardBridge = { get() },
            environmentBridge = { get() },
            systemBridge = { get() },
        )
    }
}