package ru.raydroid.plugin.host.api

import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge

sealed interface NotificationEvent {
    data class Alert(
        val title: UiText,
        val message: UiText,
        val actions: List<NotificationServiceBridge.AlertAction>
        ) : NotificationEvent
    data class AlertResult(val action: NotificationServiceBridge.AlertAction) : NotificationEvent
    data class ShowToast(val toast: Any) : NotificationEvent
    data class HideToast(val toast: Any): NotificationEvent
}