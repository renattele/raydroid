package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionNotificationServiceBridge(
    private val notificationServiceBridge: NotificationServiceBridge,
    private val manifest: Manifest
): NotificationServiceBridge {
    override suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: NotificationServiceBridge.AlertAction,
        dismissAction: NotificationServiceBridge.AlertAction?
    ): NotificationServiceBridge.AlertAction? {
        if (manifest.access.notification?.showAlerts == true) {
            return notificationServiceBridge.alert(title, message, confirmAction, dismissAction)
        }
        throw PermissionDenied()
    }

    override suspend fun showToast(toast: NotificationServiceBridge.Toast) {
        if (manifest.access.notification?.showToasts == true) {
            notificationServiceBridge.showToast(toast)
        }
    }

    override suspend fun hideToast(toast: NotificationServiceBridge.Toast) {
        if (manifest.access.notification?.showToasts == true) {
            notificationServiceBridge.hideToast(toast)
        }
    }
}