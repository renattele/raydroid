package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.core.UiText

interface NotificationService {
    suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: AlertAction,
        dismissAction: AlertAction? = null
    ): AlertAction?

    suspend fun showToast(toast: Toast)
    suspend fun hideToast(toast: Toast)

    @Serializable
    data class AlertAction(
        val title: UiText,
        val style: Style
    ) {
        enum class Style {
            Default,
            Destructive,
            Cancel
        }
    }


    @Serializable
    data class Toast(
        val message: UiText,
        val style: Style
    ) {
        enum class Style {
            Animated,
            Success,
            Failure
        }
    }
}


