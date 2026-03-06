package ru.raydroid.plugin.host.impl.datasource.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class ListItemCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertListItem(entity: ListItemCacheEntity): Long

    @Query("DELETE FROM list_item_cache WHERE plugin_id = :pluginId AND command = :command AND item_id = :itemId")
    protected abstract suspend fun deleteListItem(pluginId: String, command: String, itemId: String)

    @Update
    protected abstract suspend fun updateListItem(entity: ListItemCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertContent(entities: List<ListItemCacheContentEntity>)

    @Query("SELECT id FROM list_item_cache WHERE plugin_id = :pluginId AND command = :command AND item_id = :itemId LIMIT 1")
    protected abstract suspend fun getListItemId(pluginId: String, command: String, itemId: String): Long?

    @Query("DELETE FROM list_item_cache_content WHERE list_item_cache_id = :listItemCacheId")
    protected abstract suspend fun deleteContentByListItemCacheId(listItemCacheId: Long)

    @Transaction
    open suspend fun insert(entity: ListItemCacheWithContent) {
        val existingId = getListItemId(
            pluginId = entity.listItemCache.pluginId,
            command = entity.listItemCache.command,
            itemId = entity.listItemCache.itemId
        )
        val listItemCacheId = if (existingId == null) {
            insertListItem(entity.listItemCache)
        } else {
            updateListItem(entity.listItemCache.copy(id = existingId))
            existingId
        }

        deleteContentByListItemCacheId(listItemCacheId)
        if (entity.content.isNotEmpty()) {
            insertContent(entity.content.map { content ->
                content.copy(id = 0, listItemCacheId = listItemCacheId)
            })
        }
    }

    @Transaction
    open suspend fun delete(pluginId: String, command: String, itemId: String) {
        getListItemId(pluginId = pluginId, command = command, itemId = itemId)
            ?.let { listItemCacheId ->
                deleteContentByListItemCacheId(listItemCacheId)
            }
        deleteListItem(pluginId = pluginId, command = command, itemId = itemId)
    }

    @Query(
        """
        SELECT
            list_item_cache.plugin_id AS plugin_id,
            list_item_cache.command AS command,
            list_item_cache.item_id AS item_id,
            list_item_cache.icon AS icon,
            list_item_cache.icon_type AS icon_type,
            list_item_cache_content.title AS title,
            list_item_cache_content.description AS description
        FROM list_item_cache
        INNER JOIN (
            SELECT
                list_item_cache.id AS list_item_cache_id,
                MIN(list_item_cache_content.id) AS content_id
            FROM list_item_cache
            INNER JOIN list_item_cache_content
                ON list_item_cache.id = list_item_cache_content.list_item_cache_id
            WHERE list_item_cache_content.title LIKE '%' || :query || '%'
                OR list_item_cache_content.description LIKE '%' || :query || '%'
            GROUP BY list_item_cache.id
            ORDER BY MIN(list_item_cache_content.id)
            LIMIT :limit
        ) AS matched_items
            ON list_item_cache.id = matched_items.list_item_cache_id
        INNER JOIN list_item_cache_content
            ON list_item_cache_content.id = matched_items.content_id
        ORDER BY matched_items.content_id
        """
    )
    abstract fun search(query: String, limit: Int): Flow<List<ListItemCacheSearchEntity>>
}
