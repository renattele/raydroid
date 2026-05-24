package ru.raydroid.plugin.host.impl.data.search.cache

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect val dbPlatformModule: Module
internal val dbModule =
    module {
        includes(dbPlatformModule)
        single<PluginDatabase> {
            getAppDatabase(get())
        }
        single<SearchIndexCacheDao> {
            get<PluginDatabase>().getSearchIndexCacheDao()
        }
    }
