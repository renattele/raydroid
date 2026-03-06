package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.host.api.ListItemUpdate
import ru.raydroid.plugin.host.api.SearchRepository
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheDao
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheContentEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheSearchEntity
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheWithContent

internal class SearchRepositoryImpl(
    private val cacheDao: ListItemCacheDao,
    private val resourceResolver: SearchResourceResolver
) : SearchRepository {
    override suspend fun update(updateList: List<ListItemUpdate>) {
        val session = resourceResolver.session()
        updateList.forEach { updateItem ->
            if (updateItem is ListItemUpdate.Upsert) {
                cacheDao.insert(updateItem.toCacheEntity(session))
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

    private suspend fun ListItemUpdate.Upsert.toCacheEntity(
        session: SearchResourceResolver.Session
    ): ListItemCacheWithContent {
        val resolvedIcon = session.resolveIcon(pluginId, item.icon)
        val resolvedContent = session.resolveContent(
            pluginId = pluginId,
            title = item.title,
            description = item.description
        )
        return ListItemCacheWithContent(
            listItemCache = ListItemCacheEntity(
                pluginId = pluginId.id,
                command = commandName,
                itemId = item.id.value,
                icon = resolvedIcon.value,
                iconType = resolvedIcon.type.name
            ),
            content = resolvedContent.map { content ->
                ListItemCacheContentEntity(
                    title = content.title,
                    description = content.description
                )
            }
        )
    }

    private fun ListItemCacheSearchEntity.toListItem(): ListItem {
        return ListItem(
            id = ItemId(itemId),
            icon = Icon(icon, Icon.Type.valueOf(iconType)),
            title = ru.raydroid.plugin.api.core.UiText.Plain(title),
            description = ru.raydroid.plugin.api.core.UiText.Plain(description)
        )
    }
}
