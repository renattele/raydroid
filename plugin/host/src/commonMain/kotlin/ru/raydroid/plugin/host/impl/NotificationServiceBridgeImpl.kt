package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.host.service.NotificationEvent

class NotificationServiceBridgeImpl(
    private val onEvent: (NotificationEvent) -> Unit,
    private val incomingEvents: Flow<NotificationEvent>
): NotificationServiceBridge {
    override suspend fun alert(
        title: UiText,
        message: UiText,
        actions: List<NotificationServiceBridge.AlertAction>,
        primaryAction: NotificationServiceBridge.AlertAction
    ): NotificationServiceBridge.AlertAction? {
        onEvent(NotificationEvent.Alert(title, message, actions))
        val result = incomingEvents.mapNotNull { event ->
            if (event is NotificationEvent.AlertResult) {
                event.action
            } else {
                null
            }
        }.first()
        return result
    }

    override suspend fun showToast(toast: NotificationServiceBridge.Toast) {
        onEvent(NotificationEvent.ShowToast(toast))
    }

    override suspend fun hideToast(toast: NotificationServiceBridge.Toast) {
        onEvent(NotificationEvent.HideToast(toast))
    }
}