package ru.raydroid.plugin.api.presentation

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Modifier
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
        val CommandRoot = CommandItemId("__command_root__")
    }
}

@Serializable
@JvmInline
value class CommandCallbackId(val value: String)

@Serializable
@JvmInline
value class CommandActionTarget(val itemId: CommandItemId)

@Serializable
data class CommandCallbackRef(
    val id: CommandCallbackId,
    val generation: Long
)

@Serializable
data class CommandListItem(
    val id: CommandItemId,
    val icon: Icon?,
    val title: UiText?,
    val description: UiText?,
    val enabled: Boolean = true,
    val iconColor: Color? = null,
    val trailingText: UiText? = null,
    val quickAction: CommandListQuickAction? = null,
)

@Serializable
data class CommandListQuickAction(
    val title: UiText,
    val icon: Icon
)

@Serializable
data class CommandListAction(
    val callback: CommandCallbackRef,
    val title: UiText,
    val description: UiText?,
    val icon: Icon?,
    val group: UiText? = null,
    val style: Style = Style.Default,
    val primary: Boolean = false,
    val showPrimaryHint: Boolean = true,
    val enabled: Boolean = true,
) {
    enum class Style {
        Default,
        Destructive
    }
}

interface CommandListScope {
    @Ray
    fun entry(
        id: CommandItemId = CommandItemId.random(),
        title: UiText? = null,
        description: UiText? = null,
        icon: Icon? = null,
        iconColor: Color? = null,
        trailingText: UiText? = null,
        quickAction: CommandListQuickAction? = null,
        modifier: Modifier = Modifier,
        content: RayScope.() -> Unit = {}
    )
}

interface CommandActionScope {
    @Ray
    fun group(title: UiText, content: CommandActionScope.() -> Unit)

    @Ray
    fun action(
        title: UiText,
        icon: Icon? = null,
        description: UiText? = null,
        style: CommandListAction.Style = CommandListAction.Style.Default,
        primary: Boolean = false,
        showPrimaryHint: Boolean = true,
        enabled: Boolean = true,
        onClick: suspend () -> Unit
    )
}

@Serializable
data class CommandPresentation(
    val listEntry: CommandListItem,
    val primaryCallback: CommandCallbackRef? = null,
    val actions: List<CommandListAction> = emptyList(),
    val content: List<RayNodeData>
)

typealias CommandPresentationMap = Map<CommandItemId, CommandPresentation>

internal fun buildCommandActions(
    path: String,
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
    content: CommandActionScope.() -> Unit
): List<CommandListAction> {
    return buildCommandActions(
        path = path,
        registerCallback = registerCallback,
        group = null,
        groupPath = emptyList(),
        content = content
    )
}

private fun buildCommandActions(
    path: String,
    registerCallback: (String, suspend () -> Unit) -> CommandCallbackRef,
    group: UiText?,
    groupPath: List<Int>,
    content: CommandActionScope.() -> Unit
): List<CommandListAction> {
    val actions = mutableListOf<CommandListAction>()
    var groupIndex = 0
    var actionIndex = 0
    val scope = object : CommandActionScope {
        override fun group(title: UiText, content: CommandActionScope.() -> Unit) {
            actions += buildCommandActions(
                path = path,
                registerCallback = registerCallback,
                group = title,
                groupPath = groupPath + groupIndex++,
                content = content
            )
        }

        override fun action(
            title: UiText,
            icon: Icon?,
            description: UiText?,
            style: CommandListAction.Style,
            primary: Boolean,
            showPrimaryHint: Boolean,
            enabled: Boolean,
            onClick: suspend () -> Unit
        ) {
            actions += CommandListAction(
                callback = registerCallback(
                    "$path:${groupPath.joinToString(".")}:$actionIndex",
                    onClick
                ),
                title = title,
                icon = icon,
                description = description,
                group = group,
                style = style,
                primary = primary,
                showPrimaryHint = showPrimaryHint,
                enabled = enabled
            )
            actionIndex++
        }
    }
    scope.content()
    return actions
}
