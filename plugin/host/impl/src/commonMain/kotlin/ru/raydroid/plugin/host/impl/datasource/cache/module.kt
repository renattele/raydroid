package ru.raydroid.plugin.host.impl.datasource.cache

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val dbPlatformModule: Module
internal val dbModule = module {
    includes(dbPlatformModule)
    single<PluginDatabase> {
        getAppDatabase(get())
    }
    single<ListItemCacheDao> {
        get<PluginDatabase>().getListItemCacheDao()
    }
}