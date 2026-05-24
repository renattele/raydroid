package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText

interface NotificationServiceBridge : ZiplineService {
    suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: AlertAction,
        dismissAction: AlertAction? = null,
    ): AlertAction?

    suspend fun showToast(toast: Toast): ToastHandle

    suspend fun hideToast(toastId: String)

    @Serializable
    data class AlertAction(
        val title: UiText,
        val style: Style,
    ) {
        enum class Style {
            Default,
            Destructive,
            Cancel,
        }
    }

    @Serializable
    data class Toast(
        val message: UiText,
        val style: Style,
        val autoDismissMillis: Long? = defaultAutoDismissMillis(style),
    ) {
        enum class Style {
            Animated,
            Success,
            Failure,
        }

        companion object {
            const val DEFAULT_AUTO_DISMISS_MILLIS = 5_000L

            fun defaultAutoDismissMillis(style: Style): Long? =
                when (style) {
                    Style.Animated -> null

                    Style.Success,
                    Style.Failure,
                    -> DEFAULT_AUTO_DISMISS_MILLIS
                }
        }
    }

    @Serializable
    data class ToastHandle(
        val id: String,
    )
}
