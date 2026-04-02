package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginSpacing
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.math.pow

@Composable
fun ActionPanel(
    toasts: List<NotificationEvent.ShowToast>,
    focusedItem: PluginCommandListItem?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .padding(horizontal = PluginSpacing.Medium.toDp())
    ) {
        Action(focusedItem, Modifier.weight(1f))
    }
}

@Composable
private fun Toasts(
    toasts: List<NotificationEvent.ShowToast>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        toasts.forEachIndexed { index, toast ->
            val invertedIndex = toasts.lastIndex - index
            val backgroundColor = PluginColor.SurfaceContainer.toColor()
            Toast(
                toast.toast,
                Modifier
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
                    .padding(vertical = PluginSpacing.Medium.toDp())
                    .border(
                        PluginSpacing.Border.toDp(),
                        PluginColor.Primary.toColor(),
                        RoundedCornerShape(PluginSpacing.Large.toDp())
                    )
                    .clip(RoundedCornerShape(PluginSpacing.Large.toDp()))
                    .background(PluginColor.SurfaceBright.toColor())
            )
        }
    }
}

@Composable
private fun Action(focusedItem: PluginCommandListItem?, modifier: Modifier = Modifier) {
    val showPopup = remember { mutableStateOf(false) }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            PluginSpacing.Medium.toDp(),
            Alignment.End
        )
    ) {
        if (focusedItem != null) {
            val primaryAction = remember(focusedItem) {
                focusedItem.actions.find { it.primary } ?: focusedItem.actions.firstOrNull()
            }
            if (primaryAction != null) {
                TextRenderer(
                    PluginTextData(
                        text = primaryAction.title,
                        color = PluginColor.OnSurfaceVariant,
                        fontSize = PluginFontSize.ExtraSmall
                    )
                )
                KeyHint {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = null,
                        Modifier.size(PluginIconSize.ExtraSmall.toDp()),
                        tint = PluginColor.OnPrimaryContainer.toColor()
                    )
                }
                KeyHint(
                    onClick = {
                        showPopup.value = !showPopup.value
                    },
                    color = PluginColor.TertiaryContainer.toColor()
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        Modifier.size(PluginIconSize.ExtraSmall.toDp()),
                        tint = PluginColor.OnTertiaryContainer.toColor()
                    )
                    val density = LocalDensity.current
                    val offset = with(density) {
                        PluginIconSize.ExtraSmall.toDp().roundToPx() * 3 / 4
                    }
                    Popup(
                        alignment = Alignment.BottomEnd,
                        onDismissRequest = {
                            showPopup.value = false
                        },
                        offset = IntOffset(offset, offset)
                    ) {
                        AnimatedVisibility(
                            showPopup.value,
                            enter = fadeIn() + scaleIn(
                                initialScale = 0.9f,
                                transformOrigin = TransformOrigin(1f, 1f)
                            ),
                            exit = fadeOut() + scaleOut(
                                targetScale = 0.9f,
                                transformOrigin = TransformOrigin(1f, 1f)
                            )
                        ) {
                            ActionsPopupContent(focusedItem.actions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsPopupContent(
    actions: List<PluginCommandListAction>,
    modifier: Modifier = Modifier
) {
    val groupedActions = remember(actions) {
        actions.groupBy { it.group }
            .entries.toList()
    }
    LazyColumn(
        modifier
            .clip(RoundedCornerShape(PluginSpacing.Medium.toDp()))
            .background(PluginColor.SurfaceBright.toColor().copy(alpha = 0.7f))
            .height(PopupHeight)
            .width(PopupWidth)
    ) {
        items(groupedActions) { (groupName, actionsList) ->
            if (groupName != null) {
                Text(groupName.asText())
            }
            actionsList.forEach { action ->
                ActionsPopupAction(action)
            }
        }
    }
}

@Composable
private fun ActionsPopupAction(action: PluginCommandListAction, modifier: Modifier = Modifier) {
    Row(modifier) {
        val color = when (action.style) {
            PluginCommandListAction.Style.Default -> PluginColor.OnSurface
            PluginCommandListAction.Style.Destructive -> PluginColor.Error
        }
        action.icon?.let { icon ->
            IconRenderer(
                PluginIconData(
                    icon = icon,
                    color = color
                )
            )
        }
        TextRenderer(data = PluginTextData(text = action.title, color = color))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Toast(toast: NotificationEvent.Toast, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PluginSpacing.ExtraSmall.toDp())
    ) {
        CompositionLocalProvider(LocalContentColor provides PluginColor.OnTertiaryContainer.toColor()) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {
                when (toast.style) {
                    NotificationEvent.Toast.Style.Animated -> CircularWavyProgressIndicator()
                    NotificationEvent.Toast.Style.Success -> Icon(
                        Icons.Default.Done,
                        contentDescription = null
                    )

                    NotificationEvent.Toast.Style.Failure -> Icon(
                        Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
            Text(toast.message.asText())
        }
    }
}

@Composable
private fun KeyHint(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = PluginColor.PrimaryContainer.toColor(),
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(PluginSpacing.Small.toDp()))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .background(color)
            .padding(PluginSpacing.Small.toDp())
    ) {
        content()
    }
}

private val PanelHeight = 80.dp
private val ToastHeight = 60.dp
private val PopupHeight = 120.dp
private val PopupWidth = 240.dp

@Preview
@Composable
private fun ActionPanelPreview() {
    RaydroidPreviewTheme {
        ActionPanel(
            toasts = listOf(
                NotificationEvent.ShowToast(
                    PluginId.Invalid,
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message"),
                        style = NotificationEvent.Toast.Style.Animated
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid,
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message2"),
                        style = NotificationEvent.Toast.Style.Success
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid,
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message3"),
                        style = NotificationEvent.Toast.Style.Success
                    )
                )
            ),
            focusedItem = PluginCommandListItem(
                id = CommandItemId.Static,
                icon = PluginIcon.Url("https://i.imgur.com/UVpA9a0.jpeg"),
                title = PluginUiText.Plain("Open"),
                description = PluginUiText.Plain("Open"),
                actions = listOf(
                    PluginCommandListAction(
                        id = "",
                        title = PluginUiText.Plain("Action 1"),
                        description = PluginUiText.Plain("Action 2"),
                        icon = PluginIcon.Url("https://i.imgur.com/UVpA9a0.jpeg")
                    )
                )
            )
        )
    }
}
