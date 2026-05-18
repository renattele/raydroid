package ru.raydroid.plugin.host.impl.data.search.cache

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
internal data class SearchIndexCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo("plugin_id")
    val pluginId: String,

    @ColumnInfo("command")
    val command: String,

    @ColumnInfo("item_id")
    val itemId: String,

    @ColumnInfo("icon")
    val icon: String?,

    @ColumnInfo("icon_type")
    val iconType: String?,

    @ColumnInfo("icon_color")
    val iconColor: String?,

    @ColumnInfo("last_used_at_epoch_ms")
    val lastUsedAtEpochMs: Long? = null,

    @ColumnInfo("usage_count")
    val usageCount: Long = 0,

    @ColumnInfo("outdated")
    val outdated: Boolean = false,
)

internal data class SearchIndexCacheWithContent(
    val searchIndexCache: SearchIndexCacheEntity,
    val content: List<SearchIndexCacheContentEntity>
)

internal sealed interface SearchIndexCacheMutation {
    data class Upsert(
        val entity: SearchIndexCacheWithContent
    ) : SearchIndexCacheMutation

    data class Delete(
        val pluginId: String,
        val commandName: String,
        val itemId: String
    ) : SearchIndexCacheMutation

    data class MarkAsOutdated(
        val pluginId: String,
        val commandName: String,
        val itemId: String
    ) : SearchIndexCacheMutation

    data class MarkAllAsOutdated(
        val pluginId: String,
        val commandName: String
    ) : SearchIndexCacheMutation

    data class ClearOutdated(
        val pluginId: String,
        val commandName: String
    ) : SearchIndexCacheMutation
}

internal data class SearchIndexCacheSearchEntity(
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
    val icon: String?,

    @ColumnInfo("icon_type")
    val iconType: String?,

    @ColumnInfo("icon_color")
    val iconColor: String?,

    @ColumnInfo("title")
    val title: String?,

    @ColumnInfo("description")
    val description: String?,

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
internal data class SearchIndexCacheContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo("list_item_cache_id")
    val searchIndexCacheId: Long = 0,

    @ColumnInfo("title")
    val title: String?,

    @ColumnInfo("description")
    val description: String?,

    @ColumnInfo("title_search")
    val titleSearch: String,

    @ColumnInfo("description_search")
    val descriptionSearch: String,

    @ColumnInfo("acronym_search")
    val acronymSearch: String
)

@Entity(tableName = "list_item_cache_content_fts")
@Fts4(
    contentEntity = SearchIndexCacheContentEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    prefix = [2, 3, 4]
)
internal data class SearchIndexCacheContentFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,

    @ColumnInfo("title_search")
    val titleSearch: String,

    @ColumnInfo("description_search")
    val descriptionSearch: String,

    @ColumnInfo("acronym_search")
    val acronymSearch: String
)
