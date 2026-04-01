package ru.raydroid.plugin.host.api.event

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId

sealed interface NotificationEvent {
    data class Alert(
        val pluginId: PluginId,
        val title: UiText,
        val message: UiText,
        val confirmAction: NotificationServiceBridge.AlertAction,
        val dismissAction: NotificationServiceBridge.AlertAction? = null,
    ) : NotificationEvent

    data class AlertResult(val action: NotificationServiceBridge.AlertAction?) : NotificationEvent
    data class ShowToast(
        val pluginId: PluginId,
        val toast: NotificationServiceBridge.Toast
    ) : NotificationEvent

    data class HideToast(
        val pluginId: PluginId,
        val toast: NotificationServiceBridge.Toast
    ) : NotificationEvent
}
