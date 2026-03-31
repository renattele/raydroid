package ru.raydroid.plugin.api.host.impl

import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.service.NotificationService

internal class NotificationServiceImpl(
    private val bridge: NotificationServiceBridge
): NotificationService {
    override suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: NotificationService.AlertAction,
        dismissAction: NotificationService.AlertAction?
    ): NotificationService.AlertAction? {
        return bridge.alert(
            title = title,
            message = message,
            confirmAction = confirmAction.toBridgeAction(),
            dismissAction = dismissAction?.toBridgeAction()
        )?.toServiceAction()
    }

    override suspend fun showToast(toast: NotificationService.Toast) {
        bridge.showToast(toast.toBridgeToast())
    }

    override suspend fun hideToast(toast: NotificationService.Toast) {
        bridge.hideToast(toast.toBridgeToast())
    }

    private fun NotificationServiceBridge.AlertAction.toServiceAction() = NotificationService.AlertAction(
        title = title,
        style = style.toServiceStyle()
    )

    private fun NotificationService.AlertAction.toBridgeAction() = NotificationServiceBridge.AlertAction(
        title = title,
        style = style.toBridgeStyle()
    )

    private fun NotificationServiceBridge.AlertAction.Style.toServiceStyle() = when (this) {
        NotificationServiceBridge.AlertAction.Style.Default -> NotificationService.AlertAction.Style.Default
        NotificationServiceBridge.AlertAction.Style.Destructive -> NotificationService.AlertAction.Style.Destructive
        NotificationServiceBridge.AlertAction.Style.Cancel -> NotificationService.AlertAction.Style.Cancel
    }

    private fun NotificationService.AlertAction.Style.toBridgeStyle() = when (this) {
        NotificationService.AlertAction.Style.Default -> NotificationServiceBridge.AlertAction.Style.Default
        NotificationService.AlertAction.Style.Destructive -> NotificationServiceBridge.AlertAction.Style.Destructive
        NotificationService.AlertAction.Style.Cancel -> NotificationServiceBridge.AlertAction.Style.Cancel
    }

    private fun NotificationServiceBridge.Toast.toServiceToast() = NotificationService.Toast(
        message = message,
        style = style.toServiceStyle()
    )

    private fun NotificationService.Toast.toBridgeToast() = NotificationServiceBridge.Toast(
        message = message,
        style = style.toBridgeStyle()
    )

    private fun NotificationServiceBridge.Toast.Style.toServiceStyle() = when (this) {
        NotificationServiceBridge.Toast.Style.Animated -> NotificationService.Toast.Style.Animated
        NotificationServiceBridge.Toast.Style.Success -> NotificationService.Toast.Style.Success
        NotificationServiceBridge.Toast.Style.Failure -> NotificationService.Toast.Style.Failure
    }

    private fun NotificationService.Toast.Style.toBridgeStyle() = when (this) {
        NotificationService.Toast.Style.Animated -> NotificationServiceBridge.Toast.Style.Animated
        NotificationService.Toast.Style.Success -> NotificationServiceBridge.Toast.Style.Success
        NotificationService.Toast.Style.Failure -> NotificationServiceBridge.Toast.Style.Failure
    }
}