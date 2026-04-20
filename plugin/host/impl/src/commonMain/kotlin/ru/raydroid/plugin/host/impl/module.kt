package ru.raydroid.plugin.host.impl

import kotlin.time.Clock
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.raydroid.plugin.host.api.application.usecase.EmitEventUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetEventsUseCase
import ru.raydroid.plugin.host.api.application.usecase.GetPluginsUseCase
import ru.raydroid.plugin.host.api.application.usecase.LoadRuntimesUseCase
import ru.raydroid.plugin.host.api.application.usecase.OpenItemUseCase
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.domain.repository.PluginRepository
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.service.HostBridgeFactory
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker
import ru.raydroid.plugin.host.api.event.EventGateway
import ru.raydroid.plugin.host.impl.data.plugin.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.LocalPluginDataSourceImpl
import ru.raydroid.plugin.host.impl.data.plugin.PluginRepositoryImpl
import ru.raydroid.plugin.host.impl.data.plugin.RemotePluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.RemotePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.data.plugin.ResourcePluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.ResourcePluginDataSourceImpl
import ru.raydroid.plugin.host.impl.data.search.CachedSearchRanker
import ru.raydroid.plugin.host.impl.data.search.SearchIndexRepositoryImpl
import ru.raydroid.plugin.host.impl.data.search.SearchRanker
import ru.raydroid.plugin.host.impl.data.search.SearchResourceResolver
import ru.raydroid.plugin.host.impl.data.search.SearchResourceResolverImpl
import ru.raydroid.plugin.host.impl.data.search.cache.dbModule
import ru.raydroid.plugin.host.impl.event.EventGatewayImpl
import ru.raydroid.plugin.host.impl.runtime.HostBridgeFactoryImpl
import ru.raydroid.plugin.host.impl.runtime.PluginLoaderImpl
import ru.raydroid.plugin.host.impl.runtime.PluginRuntimeRegistryImpl
import ru.raydroid.plugin.host.impl.services.hostServiceModule

internal expect val pluginPlatformModule: Module

val pluginHostModule = module {
    includes(pluginPlatformModule)
    includes(hostServiceModule)
    includes(dbModule)

    single<PluginLoader> {
        PluginLoaderImpl(
            dispatcher = { get() },
            json = get(),
            coroutineScope = get(),
            hostFactory = get()
        )
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
    singleOf(::CachedSearchRanker)
    single<SearchRanker> { get<CachedSearchRanker>() }
    single<SearchResultRanker> { get<CachedSearchRanker>() }
    singleOf(::SearchResourceResolverImpl) bind SearchResourceResolver::class
    singleOf(::RemotePluginDataSourceImpl) bind RemotePluginDataSource::class
    singleOf(::PluginRepositoryImpl) bind PluginRepository::class
    singleOf(::EventGatewayImpl) bind EventGateway::class
    singleOf(::SearchIndexRepositoryImpl) bind SearchIndexRepository::class
    singleOf(::PluginRuntimeRegistryImpl) bind PluginRuntimeRegistry::class

    singleOf(::GetEventsUseCase)
    singleOf(::EmitEventUseCase)
    singleOf(::SyncCacheUseCase)
    singleOf(::LoadRuntimesUseCase)
    singleOf(::SearchUseCase)
    singleOf(::OpenItemUseCase)
    singleOf(::GetPluginsUseCase)

    single<HostBridgeFactory> {
        HostBridgeFactoryImpl(
            networkBridge = { get { parametersOf(it) } },
            notificationBridge = { get { parametersOf(it) } },
            preferencesBridge = { get { parametersOf(it) } },
            cacheBridge = { get { parametersOf(it) } },
            storageBridge = { get { parametersOf(it) } },
            clipboardBridge = { get { parametersOf(it) } },
            environmentBridge = { get { parametersOf(it) } },
            systemBridge = { get { parametersOf(it) } },
        )
    }
}
