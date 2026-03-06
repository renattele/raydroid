package ru.raydroid.plugin.host.impl.datasource.cache

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ListItemCacheEntity::class,
        ListItemCacheContentEntity::class,
        ListItemCacheContentFtsEntity::class
    ],
    version = 2
)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class PluginDatabase : RoomDatabase() {
    abstract fun getListItemCacheDao(): ListItemCacheDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<PluginDatabase> {
    override fun initialize(): PluginDatabase
}

internal fun getAppDatabase(
    builder: RoomDatabase.Builder<PluginDatabase>
): PluginDatabase {
    return builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
