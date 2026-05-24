package ru.raydroid.plugin.host.impl.data.search.cache

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

internal fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<PluginDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("plugins.db")
    return Room.databaseBuilder<PluginDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
