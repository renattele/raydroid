package ru.raydroid.plugin.host.impl.data.search.cache

import androidx.room.Room
import androidx.room.RoomDatabase
import okio.Path

internal fun getDatabaseBuilder(path: Path): RoomDatabase.Builder<PluginDatabase> =
    Room.databaseBuilder<PluginDatabase>(
        name = path.toString(),
    )
