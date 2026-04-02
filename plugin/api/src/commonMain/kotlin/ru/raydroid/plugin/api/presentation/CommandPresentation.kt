package ru.raydroid.plugin.api.presentation

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Ray
import ru.raydroid.plugin.api.ui.RayNodeData
import ru.raydroid.plugin.api.ui.RayScope
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class CommandItemId(val value: String) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun random() = CommandItemId(Uuid.generateV4().toHexString())

        val Static = CommandItemId("")
    }
}

@Serializable
data class CommandListItem(
    val id: CommandItemId,
    val icon: Icon?,
    val title: UiText?,
    val description: UiText?,
    val actions: List<CommandListAction> = emptyList(),
)

@Serializable
data class CommandListAction(
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

interface CommandListScope {
    @Ray
    fun entry(
        id: CommandItemId,
        title: UiText? = null,
        description: UiText? = null,
        icon: Icon? = null,
        actions: CommandActionScope.() -> Unit = {},
        content: RayScope.() -> Unit
    )
}

interface CommandActionScope {
    @Ray
    fun group(title: UiText, content: CommandActionScope.() -> Unit)

    @Ray
    fun action(
        id: String,
        title: UiText,
        icon: Icon? = null,
        description: UiText? = null,
        style: CommandListAction.Style = CommandListAction.Style.Default,
        primary: Boolean = false
    )
}

@Serializable
data class CommandPresentation(
    val listEntry: CommandListItem,
    val content: List<RayNodeData>
)

typealias CommandPresentationMap = Map<CommandItemId, CommandPresentation>
