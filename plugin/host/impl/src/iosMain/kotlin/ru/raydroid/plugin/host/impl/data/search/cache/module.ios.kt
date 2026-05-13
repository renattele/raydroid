package ru.raydroid.plugin.host.impl.data.search.cache

import androidx.room.Room
import androidx.room.RoomDatabase
import okio.Path
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val dbPlatformModule = module {
    single<RoomDatabase.Builder<PluginDatabase>> {
        val dbPath = get<Path>(named("localPath")) / "plugins.db"
        Room.databaseBuilder<PluginDatabase>(
            name = dbPath.toString()
        )
    }
}
