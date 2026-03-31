package ru.raydroid.plugin.host.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge.Toast.Style.*
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.host.api.NotificationEvent
import ru.raydroid.plugin.host.api.PluginId
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ActionPanel(toasts: List<NotificationEvent.ShowToast>, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    Row(
        modifier
            .height(PanelHeight)
            .background(themeResolver.color(Color.SurfaceContainer))
    ) {
        Toasts(toasts, Modifier.weight(1f))
        Action(Modifier.weight(1f))
    }
}

@Composable
private fun Toasts(
    toasts: List<NotificationEvent.ShowToast>,
    modifier: Modifier = Modifier,
) {
    val themeResolver = LocalThemeResolver.current
    Box(modifier = modifier) {
        toasts.forEachIndexed { index, toast ->
            val invertedIndex = toasts.lastIndex - index
            val backgroundColor = themeResolver.color(Color.SurfaceContainer)
            Toast(
                toast.toast, Modifier
                    .graphicsLayer {
                        val scale = 1f - (invertedIndex / 20f)
                        translationY =
                            invertedIndex / ((1.3.pow(invertedIndex - 1))).toFloat() * 10.dp.toPx()
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(backgroundColor.copy(alpha = invertedIndex / 3f))
                    }
                    .padding(themeResolver.spacing(Spacing.Medium))
                    .border(
                        themeResolver.spacing(Spacing.Border),
                        themeResolver.color(Color.Primary),
                        RoundedCornerShape(themeResolver.spacing(Spacing.Large))
                    )
                    .clip(RoundedCornerShape(themeResolver.spacing(Spacing.Large)))
                    .background(themeResolver.color(Color.SurfaceBright))
                    .height(ToastHeight)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Action(modifier: Modifier = Modifier) {
    Row(modifier) {

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Toast(toast: NotificationServiceBridge.Toast, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    Row(
        modifier.padding(horizontal = themeResolver.spacing(Spacing.Medium)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            themeResolver.spacing(Spacing.ExtraSmall)
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides themeResolver.color(Color.OnTertiaryContainer)) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {
                when (toast.style) {
                    Animated -> {
                        CircularWavyProgressIndicator()
                    }

                    Success -> {
                        Icon(Icons.Default.Done, contentDescription = null)
                    }

                    Failure -> {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            }
            Text(toast.message.asText())
        }
    }
}

private fun NotificationServiceBridge.Toast.Style.backgroundColor() = when (this) {
    Animated -> Color.Transparent
    Success -> Color.Primary
    Failure -> Color.Error
}

private val PanelHeight = 80.dp
private val ToastHeight = 60.dp

@Preview
@Composable
private fun ActionPanelPreview() {
    RaydroidPreviewTheme {
        ActionPanel(
            toasts = listOf(
                NotificationEvent.ShowToast(
                    PluginId.Invalid, toast = NotificationServiceBridge.Toast(
                        message = UiText.Plain("Message"),
                        style = Animated
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid, toast = NotificationServiceBridge.Toast(
                        message = UiText.Plain("Message2"),
                        style = Success
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid, toast = NotificationServiceBridge.Toast(
                        message = UiText.Plain("Message3"),
                        style = Success
                    )
                )
            )
        )
    }
}