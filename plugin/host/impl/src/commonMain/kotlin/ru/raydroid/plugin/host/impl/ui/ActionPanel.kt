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
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import raydroid.plugin.host.impl.generated.resources.Res
import raydroid.plugin.host.impl.generated.resources.search_field_placeholder
import ru.raydroid.plugin.api.core.ItemId
import ru.raydroid.plugin.api.core.ListItem
import ru.raydroid.plugin.api.core.ListItemAction
import ru.raydroid.plugin.api.core.UiText
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge
import ru.raydroid.plugin.api.host.bridge.NotificationServiceBridge.Toast.Style.*
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.TextData
import ru.raydroid.plugin.host.api.NotificationEvent
import ru.raydroid.plugin.host.api.PluginId
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ActionPanel(
    toasts: List<NotificationEvent.ShowToast>,
    focusedItem: ListItem?,
    modifier: Modifier = Modifier
) {
    val themeResolver = LocalThemeResolver.current
    Row(
        modifier
            .height(PanelHeight)
            .background(themeResolver.color(Color.SurfaceContainer))
            .padding(horizontal = themeResolver.spacing(Spacing.Medium))
    ) {
        Toasts(toasts, Modifier.weight(1f))
        Action(focusedItem, Modifier.fillMaxHeight().weight(1f))
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
                    .padding(vertical = themeResolver.spacing(Spacing.Medium))
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
private fun Action(focusedItem: ListItem?, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            themeResolver.spacing(Spacing.Medium),
            Alignment.End
        )
    ) {
        if (focusedItem != null) {
            val primaryAction = remember(focusedItem) {
                focusedItem.actions.find { it.primary } ?: focusedItem.actions.firstOrNull()
            }
            if (primaryAction != null) {
                TextRenderer(
                    TextData(
                        text = primaryAction.title,
                        color = Color.OnSurfaceVariant,
                        fontSize = FontSize.Small
                    )
                )
                KeyHint {
                    Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Toast(toast: NotificationServiceBridge.Toast, modifier: Modifier = Modifier) {
    val themeResolver = LocalThemeResolver.current
    Row(
        modifier,
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

@Composable
private fun KeyHint(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val themeResolver = LocalThemeResolver.current
    Box(
        modifier
            .clip(RoundedCornerShape(themeResolver.spacing(Spacing.Medium)))
            .background(themeResolver.color(Color.SurfaceContainerLowest))
            .padding(themeResolver.spacing(Spacing.Small))
    ) {
        content()
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
            ),
            focusedItem = ListItem(
                id = ItemId.Static,
                icon = Icon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
                title = UiText.Plain("Open"),
                description = UiText.Plain("Open"),
                actions = listOf(
                    ListItemAction(
                        id = "",
                        title = UiText.Plain("Action 1"),
                        description = UiText.Plain("Action 2"),
                        icon = Icon.Url("https://i.imgur.com/UVpA9a0.jpeg")
                    )
                )
            )
        )
    }
}