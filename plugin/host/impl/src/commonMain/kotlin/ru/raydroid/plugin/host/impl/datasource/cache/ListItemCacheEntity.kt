package ru.raydroid.plugin.host.impl.datasource.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "list_item_cache", indices = [
    Index(
        value = ["plugin_id", "command", "item_id"],
        unique = true
    )
])
internal data class ListItemCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo("plugin_id")
    val pluginId: String,

    @ColumnInfo("command")
    val command: String,

    @ColumnInfo("item_id")
    val itemId: String,

    @ColumnInfo("icon")
    val icon: String,

    @ColumnInfo("icon_type")
    val iconType: String,
)

internal data class ListItemCacheWithContent(
    val listItemCache: ListItemCacheEntity,
    val content: List<ListItemCacheContentEntity>
)

internal data class ListItemCacheSearchEntity(
    @ColumnInfo("plugin_id")
    val pluginId: String,

    @ColumnInfo("command")
    val command: String,

    @ColumnInfo("item_id")
    val itemId: String,

    @ColumnInfo("icon")
    val icon: String,

    @ColumnInfo("icon_type")
    val iconType: String,

    @ColumnInfo("title")
    val title: String,

    @ColumnInfo("description")
    val description: String
)

@Entity(
    tableName = "list_item_cache_content",
    indices = [
        Index(value = ["list_item_cache_id"])
    ]
)
internal data class ListItemCacheContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo("list_item_cache_id")
    val listItemCacheId: Long = 0,

    @ColumnInfo("title")
    val title: String,

    @ColumnInfo("description")
    val description: String
)
