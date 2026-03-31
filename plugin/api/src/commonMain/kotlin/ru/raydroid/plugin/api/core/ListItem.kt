package ru.raydroid.plugin.api.core

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Ray
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayScope
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ItemId(val value: String) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun random() = ItemId(Uuid.generateV4().toHexString())

        val Static = ItemId("")
    }
}

@Serializable
data class ListItem(
    val id: ItemId,
    val icon: Icon?,
    val title: UiText?,
    val description: UiText?,
    val actions: List<ListItemAction> = emptyList()
)

@Serializable
data class ListItemAction(
    val id: String,
    val title: UiText,
    val description: UiText?,
    val icon: Icon,
)

interface RayListScope {
    @Ray
    fun item(
        id: ItemId,
        title: UiText? = null,
        description: UiText? = null,
        icon: Icon? = null,
        actions: RayListActionScope.() -> Unit = {},
        content: RayScope.() -> Unit
    )
}

interface RayListActionScope {
    @Ray
    fun action(id: String, title: UiText, icon: Icon, description: UiText? = null)
}

typealias RayItem = Pair<ItemId, List<RayNodeData>>

typealias RayItems = Map<ListItem, List<RayNodeData>>