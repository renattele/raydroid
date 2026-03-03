package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.SearchRepository
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheDao
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheContentEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheSearchEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheWithContent

internal class SearchRepositoryImpl(
    private val cacheDao: ListItemCacheDao
) : SearchRepository {
    override suspend fun update(updateList: List<ListItemUpdate>) {
        updateList.forEach { updateItem ->
            if (updateItem is ListItemUpdate.Upsert) {
                cacheDao.insert(updateItem.toCacheEntity())
            } else if (updateItem is ListItemUpdate.Delete) {
                cacheDao.delete(
                    pluginId = updateItem.pluginId.id,
                    command = updateItem.commandName,
                    itemId = updateItem.itemId.value
                )
            }
        }
    }

    override fun search(
        query: String,
        limit: Int
    ): Flow<List<ListItem>> = cacheDao.search(query, limit).map { foundResults ->
        foundResults.map { foundResult ->
            foundResult.toListItem()
        }
    }

    private fun ListItemUpdate.Upsert.toCacheEntity(): ListItemCacheWithContent =
        ListItemCacheWithContent(
            listItemCache = ListItemCacheEntity(
                pluginId = pluginId.id,
                command = commandName,
                itemId = item.id.value,
                icon = item.icon.value,
                iconType = item.icon.type.name
            ),
            content = listOf(
                ListItemCacheContentEntity(
                    title = item.title.text,
                    description = item.description.text
                )
            )
        )

    private fun ListItemCacheSearchEntity.toListItem(): ListItem {
        return ListItem(
            id = ItemId(itemId),
            icon = Icon(icon, Icon.Type.valueOf(iconType)),
            title = UiText.Plain(title),
            description = UiText.Plain(description)
        )
    }
}
