package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.EventGateway

class NotificationServiceBridgeImpl(
    private val eventGateway: EventGateway,
    private val pluginId: PluginId
) : NotificationServiceBridge {
    override suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: NotificationServiceBridge.AlertAction,
        dismissAction: NotificationServiceBridge.AlertAction?
    ): NotificationServiceBridge.AlertAction {
        eventGateway.emit(
            pluginId,
            NotificationEvent.Alert(pluginId, title, message, confirmAction, dismissAction)
        )
        val result = eventGateway.get(pluginId).mapNotNull { event ->
            val data = event.data
            if (data is NotificationEvent.AlertResult) {
                data.action
            } else {
                null
            }
        }.first()
        return result
    }

    override suspend fun showToast(toast: NotificationServiceBridge.Toast) {
        eventGateway.emit(
            pluginId,
            NotificationEvent.ShowToast(pluginId, toast)
        )
    }

    override suspend fun hideToast(toast: NotificationServiceBridge.Toast) {
        eventGateway.emit(pluginId, NotificationEvent.HideToast(pluginId, toast))
    }
}