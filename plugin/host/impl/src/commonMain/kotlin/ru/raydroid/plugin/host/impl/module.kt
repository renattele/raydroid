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
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SearchRepository
import ru.raydroid.plugin.host.api.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.usecase.OpenItemUseCase
import ru.raydroid.plugin.host.api.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.datasource.LocalPluginDataSourceImpl
import ru.raydroid.plugin.host.impl.datasource.RemotePluginDataSource
import ru.raydroid.plugin.host.impl.datasource.RemotePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.datasource.ResourcePluginDataSource
import ru.raydroid.plugin.host.impl.datasource.ResourcePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.datasource.cache.dbModule
import ru.raydroid.plugin.host.impl.services.hostServiceModule
import kotlin.time.Clock

internal expect val pluginPlatformModule: Module

val pluginHostModule = module {
    includes(pluginPlatformModule)
    includes(hostServiceModule)
    includes(dbModule)
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
    single<Clock> { Clock.System }
    single<ResourcePluginDataSource> {
        ResourcePluginDataSourceImpl(get())
    }
    singleOf(::CachedSearchRanker) bind SearchRanker::class
    singleOf(::SearchResourceResolverImpl) bind SearchResourceResolver::class
    singleOf(::RemotePluginDataSourceImpl) {
        bind<RemotePluginDataSource>()
    }
    singleOf(::PluginRepositoryImpl) {
        bind<PluginRepository>()
    }
    singleOf(::SearchRepositoryImpl) bind SearchRepository::class
    singleOf(::PluginRuntimeManagerImpl) bind PluginRuntimeManager::class

    singleOf(::SyncCacheUseCase)
    singleOf(::LoadRuntimesUseCase)
    singleOf(::SearchUseCase)
    singleOf(::OpenItemUseCase)
    singleOf(::GetPluginsUseCase)

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
