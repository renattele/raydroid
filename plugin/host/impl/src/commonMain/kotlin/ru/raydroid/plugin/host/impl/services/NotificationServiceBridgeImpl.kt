package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.EventGateway
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.impl.ui.toPluginUiText

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
            NotificationEvent.Alert(
                pluginId = pluginId,
                title = title.toPluginUiText(pluginId),
                message = message.toPluginUiText(pluginId),
                confirmAction = confirmAction.toNotificationAction(),
                dismissAction = dismissAction?.toNotificationAction()
            )
        )
        val selection = eventGateway.get(pluginId).mapNotNull { event ->
            val data = event.data
            if (data is NotificationEvent.AlertResult) {
                data.selection
            } else {
                null
            }
        }.first()
        return when (selection) {
            NotificationEvent.Selection.Confirm -> confirmAction
            NotificationEvent.Selection.Dismiss -> dismissAction
        } ?: confirmAction
    }

    override suspend fun showToast(toast: NotificationServiceBridge.Toast) {
        eventGateway.emit(
            pluginId,
            NotificationEvent.ShowToast(pluginId, toast.toNotificationToast())
        )
    }

    override suspend fun hideToast(toast: NotificationServiceBridge.Toast) {
        eventGateway.emit(pluginId, NotificationEvent.HideToast(pluginId, toast.toNotificationToast()))
    }

    private fun NotificationServiceBridge.AlertAction.toNotificationAction() = NotificationEvent.AlertAction(
        title = title.toPluginUiText(pluginId),
        style = when (style) {
            NotificationServiceBridge.AlertAction.Style.Default -> NotificationEvent.AlertAction.Style.Default
            NotificationServiceBridge.AlertAction.Style.Destructive -> NotificationEvent.AlertAction.Style.Destructive
            NotificationServiceBridge.AlertAction.Style.Cancel -> NotificationEvent.AlertAction.Style.Cancel
        }
    )

    private fun NotificationServiceBridge.Toast.toNotificationToast() = NotificationEvent.Toast(
        message = message.toPluginUiText(pluginId),
        style = when (style) {
            NotificationServiceBridge.Toast.Style.Animated -> NotificationEvent.Toast.Style.Animated
            NotificationServiceBridge.Toast.Style.Success -> NotificationEvent.Toast.Style.Success
            NotificationServiceBridge.Toast.Style.Failure -> NotificationEvent.Toast.Style.Failure
        }
    )
}
