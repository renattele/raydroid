package ru.raydroid.plugin.host.api.ui

import ru.raydroid.plugin.api.presentation.CommandCallbackRef
import ru.raydroid.plugin.api.presentation.CommandItemId

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
)

data class PluginCommandListAction(
    val callback: PluginCommandCallback,
    val title: PluginUiText,
    val description: PluginUiText?,
    val icon: PluginIcon?,
    val group: PluginUiText? = null,
    val style: Style = Style.Default,
    val primary: Boolean = false,
    val enabled: Boolean = true,
) {
    enum class Style {
        Default,
        Destructive
    }
}

data class PluginCommandPresentation(
    val listEntry: PluginCommandListItem,
    val primaryCallback: PluginCommandCallback?,
    val actions: List<PluginCommandListAction> = emptyList(),
    val content: List<PluginRayNodeData>
)
