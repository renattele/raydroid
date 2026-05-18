package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandItemId

interface ActionPanelActionUi {
    val title: PluginUiText
    val description: PluginUiText?
    val icon: PluginIcon?
    val group: PluginUiText?
    val primary: Boolean
    val showPrimaryHint: Boolean
    val enabled: Boolean
    val destructive: Boolean
}

class PluginCommandCallback(
    val ref: CommandCallbackRef,
    private val dispatch: suspend (CommandCallbackRef) -> Unit
) {
    suspend operator fun invoke() {
        dispatch(ref)
    }

    override fun equals(other: Any?): Boolean {
        return other is PluginCommandCallback && ref == other.ref
    }

    override fun hashCode(): Int {
        return ref.hashCode()
    }

    override fun toString(): String {
        return "PluginCommandCallback(ref=$ref)"
    }
}

data class PluginCommandListItem(
    val id: CommandItemId,
    val icon: PluginIcon?,
    val title: PluginUiText?,
    val description: PluginUiText?,
    val enabled: Boolean = true,
    val iconColor: PluginColor? = null,
    val trailingText: PluginUiText? = null,
    val alias: String? = null,
    val quickAction: PluginCommandListQuickAction? = null,
)

data class PluginCommandListQuickAction(
    val title: PluginUiText,
    val icon: PluginIcon
)

data class PluginCommandListAction(
    val callback: PluginCommandCallback,
    override val title: PluginUiText,
    override val description: PluginUiText?,
    override val icon: PluginIcon?,
    override val group: PluginUiText? = null,
    val style: Style = Style.Default,
    override val primary: Boolean = false,
    override val showPrimaryHint: Boolean = true,
    override val enabled: Boolean = true,
) : ActionPanelActionUi {
    enum class Style {
        Default,
        Destructive
    }

    override val destructive: Boolean
        get() = style == Style.Destructive
}

data class PluginCommandPresentation(
    val listEntry: PluginCommandListItem,
    val primaryCallback: PluginCommandCallback?,
    val actions: List<PluginCommandListAction> = emptyList(),
    val content: List<PluginRayNodeData>
)
