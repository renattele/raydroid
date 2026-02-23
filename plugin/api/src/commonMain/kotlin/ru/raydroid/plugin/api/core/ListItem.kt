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
    val icon: Icon,
    val title: UiText,
    val description: UiText
)

interface RayListScope {
    @Ray
    fun item(id: ItemId, content: RayScope.() -> Unit)
}

typealias RayItem = Pair<ItemId, List<RayNodeData>>

typealias RayItems = Map<ItemId, List<RayNodeData>>