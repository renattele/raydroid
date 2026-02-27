package ru.raydroid.composeapp

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.includes
import org.koin.dsl.module
import ru.raydroid.core.data.coreDataModule
import ru.raydroid.plugin.host.api.PluginRepository
import ru.raydroid.plugin.host.impl.pluginHostModule
import ru.raydroid.search.SearchViewModel

val appModule = module {
    includes(coreDataModule)
    includes(pluginHostModule)

    viewModel<SearchViewModel> {
        SearchViewModel(get<PluginRepository>(), get())
    }
}

fun initKoin(configuration: KoinAppDeclaration? = null) {
    startKoin {
        includes(configuration)
        modules(appModule)
    }
}