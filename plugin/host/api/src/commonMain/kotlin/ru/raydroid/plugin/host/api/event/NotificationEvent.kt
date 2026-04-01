package ru.raydroid.plugin.host.api.event

import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.ui.PluginUiText

sealed interface NotificationEvent {
    data class Alert(
        val pluginId: PluginId,
        val title: PluginUiText,
        val message: PluginUiText,
        val confirmAction: AlertAction,
        val dismissAction: AlertAction? = null,
    ) : NotificationEvent

    data class AlertResult(
        val selection: Selection?
    ) : NotificationEvent

    data class AlertAction(
        val title: PluginUiText,
        val style: Style
    ) {
        enum class Style {
            Default,
            Destructive,
            Cancel
        }
    }

    enum class Selection {
        Confirm,
        Dismiss
    }

    data class ShowToast(
        val pluginId: PluginId,
        val toast: Toast
    ) : NotificationEvent

    data class HideToast(
        val pluginId: PluginId,
        val toast: Toast
    ) : NotificationEvent

    data class Toast(
        val message: PluginUiText,
        val style: Style
    ) {
        enum class Style {
            Animated,
            Success,
            Failure
        }
    }
}
