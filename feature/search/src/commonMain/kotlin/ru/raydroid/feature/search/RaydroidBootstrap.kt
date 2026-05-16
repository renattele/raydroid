package ru.raydroid.feature.search

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import ru.raydroid.core.data.coreDataModule
import ru.raydroid.plugin.host.impl.pluginHostModule

val raydroidSearchModule = module {
    includes(coreDataModule)
    includes(pluginHostModule)

    factoryOf(::GetCommandCatalogUseCase)
    factoryOf(::CommandCatalogProvider)
    factory {
        SearchViewModel(
            syncCacheUseCase = get(),
            loadRuntimesUseCase = get(),
            searchUseCase = get(),
            getPluginsUseCase = get(),
            openCommandUseCase = get(),
            enterItemUseCase = get(),
            closeCommandUseCase = get(),
            backCommandUseCase = get(),
            executeCommandCallbackUseCase = get(),
            getCommandFullscreenUseCase = get(),
            getEventsUseCase = get(),
            emitEventUseCase = get(),
            getSearchFieldRequestsUseCase = get(),
            updateCommandQueryUseCase = get()
        )
    }
}

fun initKoin(configuration: KoinAppDeclaration? = null) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    startKoin {
        includes(configuration)
        modules(raydroidSearchModule)
    }
}

object RaydroidBootstrap {
    fun initKoin() {
        ru.raydroid.feature.search.initKoin()
    }

    fun createSearchViewModel(): SearchViewModel {
        return KoinPlatformTools.defaultContext().get().get()
    }

    fun createCommandCatalogProvider(): CommandCatalogProvider {
        return KoinPlatformTools.defaultContext().get().get()
    }
}

fun InitKoin() {
    RaydroidBootstrap.initKoin()
}

fun CreateSearchViewModel(): SearchViewModel {
    return RaydroidBootstrap.createSearchViewModel()
}

fun CreateCommandCatalogProvider(): CommandCatalogProvider {
    return RaydroidBootstrap.createCommandCatalogProvider()
}
