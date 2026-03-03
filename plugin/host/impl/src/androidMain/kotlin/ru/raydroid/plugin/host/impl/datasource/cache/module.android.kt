package ru.raydroid.plugin.host.impl.datasource.cache

import androidx.room.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val dbPlatformModule: Module = module {
    single<RoomDatabase.Builder<PluginDatabase>> {
        getDatabaseBuilder(androidContext())
    }
}