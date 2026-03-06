package ru.raydroid.plugin.host.impl.datasource.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
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

    @ColumnInfo("last_used_at_epoch_ms")
    val lastUsedAtEpochMs: Long? = null,

    @ColumnInfo("usage_count")
    val usageCount: Long = 0,
)

internal data class ListItemCacheWithContent(
    val listItemCache: ListItemCacheEntity,
    val content: List<ListItemCacheContentEntity>
)

internal data class ListItemCacheSearchEntity(
    @ColumnInfo("cache_id")
    val cacheId: Long,

    @ColumnInfo("content_id")
    val contentId: Long,

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
    val description: String,

    @ColumnInfo("last_used_at_epoch_ms")
    val lastUsedAtEpochMs: Long?,

    @ColumnInfo("usage_count")
    val usageCount: Long
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

@Entity(tableName = "list_item_cache_content_fts")
@Fts4(
    contentEntity = ListItemCacheContentEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    prefix = [2, 3, 4]
)
internal data class ListItemCacheContentFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,

    @ColumnInfo("title")
    val title: String,

    @ColumnInfo("description")
    val description: String
)
