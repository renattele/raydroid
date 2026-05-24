package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText

interface NotificationService {
    suspend fun alert(
        title: UiText,
        message: UiText,
        confirmAction: AlertAction,
        dismissAction: AlertAction? = null,
    ): AlertAction?

    suspend fun showToast(toast: Toast)

    suspend fun <T> showToast(
        toast: Toast,
        block: suspend () -> T,
    ): T

    suspend fun showToast(
        message: UiText,
        style: Toast.Style = Toast.Style.Success,
        autoDismissMillis: Long? = Toast.defaultAutoDismissMillis(style),
    ) {
        showToast(
            Toast(
                message = message,
                style = style,
                autoDismissMillis = autoDismissMillis,
            ),
        )
    }

    suspend fun <T> showToast(
        message: UiText,
        style: Toast.Style = Toast.Style.Success,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T =
        showToast(
            Toast(
                message = message,
                style = style,
                autoDismissMillis = autoDismissMillis,
            ),
            block,
        )

    suspend fun showToast(
        message: String,
        style: Toast.Style = Toast.Style.Success,
        autoDismissMillis: Long? = Toast.defaultAutoDismissMillis(style),
    ) {
        showToast(
            message = UiText.Plain(message),
            style = style,
            autoDismissMillis = autoDismissMillis,
        )
    }

    suspend fun <T> showToast(
        message: String,
        style: Toast.Style = Toast.Style.Success,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T =
        showToast(
            message = UiText.Plain(message),
            style = style,
            autoDismissMillis = autoDismissMillis,
            block = block,
        )

    suspend fun showLoadingToast(
        message: UiText,
        autoDismissMillis: Long? = null,
    ) {
        showToast(message, Toast.Style.Animated, autoDismissMillis)
    }

    suspend fun <T> showLoadingToast(
        message: UiText,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showToast(message, Toast.Style.Animated, autoDismissMillis, block)

    suspend fun showLoadingToast(
        message: String,
        autoDismissMillis: Long? = null,
    ) {
        showLoadingToast(UiText.Plain(message), autoDismissMillis)
    }

    suspend fun <T> showLoadingToast(
        message: String,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showLoadingToast(UiText.Plain(message), autoDismissMillis, block)

    suspend fun showSuccessToast(
        message: UiText,
        autoDismissMillis: Long? = Toast.DEFAULT_AUTO_DISMISS_MILLIS,
    ) {
        showToast(message, Toast.Style.Success, autoDismissMillis)
    }

    suspend fun <T> showSuccessToast(
        message: UiText,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showToast(message, Toast.Style.Success, autoDismissMillis, block)

    suspend fun showSuccessToast(
        message: String,
        autoDismissMillis: Long? = Toast.DEFAULT_AUTO_DISMISS_MILLIS,
    ) {
        showSuccessToast(UiText.Plain(message), autoDismissMillis)
    }

    suspend fun <T> showSuccessToast(
        message: String,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showSuccessToast(UiText.Plain(message), autoDismissMillis, block)

    suspend fun showFailureToast(
        message: UiText,
        autoDismissMillis: Long? = Toast.DEFAULT_AUTO_DISMISS_MILLIS,
    ) {
        showToast(message, Toast.Style.Failure, autoDismissMillis)
    }

    suspend fun <T> showFailureToast(
        message: UiText,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showToast(message, Toast.Style.Failure, autoDismissMillis, block)

    suspend fun showFailureToast(
        message: String,
        autoDismissMillis: Long? = Toast.DEFAULT_AUTO_DISMISS_MILLIS,
    ) {
        showFailureToast(UiText.Plain(message), autoDismissMillis)
    }

    suspend fun <T> showFailureToast(
        message: String,
        autoDismissMillis: Long? = null,
        block: suspend () -> T,
    ): T = showFailureToast(UiText.Plain(message), autoDismissMillis, block)

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
}
