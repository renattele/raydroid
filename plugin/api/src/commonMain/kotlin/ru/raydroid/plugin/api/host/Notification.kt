package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.core.UiText

interface Notification: ZiplineService {
    suspend fun alert(options: AlertOptions): AlertAction?

    suspend fun showToast(toast: Toast)
    suspend fun hideToast(toast: Toast)

    @Serializable
    data class AlertOptions(
        val title: UiText,
        val message: UiText,
        val actions: List<AlertAction>,
        val primaryAction: AlertAction
    )

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