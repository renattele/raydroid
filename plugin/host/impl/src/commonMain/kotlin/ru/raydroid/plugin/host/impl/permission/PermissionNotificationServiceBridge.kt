package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.NotificationServiceBridge
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.model.UiText

internal class PermissionNotificationServiceBridge(
    private val notificationServiceBridge: NotificationServiceBridge,
    private val manifest: Manifest,
) : NotificationServiceBridge {
    override suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: NotificationServiceBridge.AlertAction,
        dismissAction: NotificationServiceBridge.AlertAction?,
    ): NotificationServiceBridge.AlertAction? {
        if (manifest.access.notification?.showAlerts == true) {
            return notificationServiceBridge.alert(title, message, confirmAction, dismissAction)
        }
        throw PermissionDenied()
    }

    override suspend fun showToast(toast: NotificationServiceBridge.Toast): NotificationServiceBridge.ToastHandle =
        if (manifest.access.notification?.showToasts == true) {
            notificationServiceBridge.showToast(toast)
        } else {
            NotificationServiceBridge.ToastHandle("")
        }

    override suspend fun hideToast(toastId: String) {
        if (manifest.access.notification?.showToasts == true) {
            notificationServiceBridge.hideToast(toastId)
        }
    }
}
