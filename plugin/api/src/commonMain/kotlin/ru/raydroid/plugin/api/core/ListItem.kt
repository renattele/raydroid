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
    val icon: Icon?,
    val group: UiText? = null,
    val style: Style = Style.Default,
    val primary: Boolean = false
) {
    enum class Style {
        Default,
        Destructive
    }
}

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
    fun group(title: UiText, content: RayListActionScope.() -> Unit)

    @Ray
    fun action(
        id: String,
        title: UiText,
        icon: Icon? = null,
        description: UiText? = null,
        style: ListItemAction.Style = ListItemAction.Style.Default,
        primary: Boolean = false
    )
}

@Serializable
data class RayItem(
    val listItem: ListItem,
    val content: List<RayNodeData>
)

typealias RayItems = Map<ItemId, RayItem>