package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.RaydroidMotionToken
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
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
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.math.pow

@Composable
fun ActionPanel(
    focusedItem: PluginCommandListItem?,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RaydroidTheme.spacing
    Row(
        modifier
            .padding(horizontal = spacing.medium)
    ) {
        Action(
            focusedItem = focusedItem,
            showActions = showActions,
            onToggleActions = onToggleActions,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ToastsOverlay(
    toasts: List<NotificationEvent.ShowToast>,
    modifier: Modifier = Modifier,
) {
    val spacing = RaydroidTheme.spacing
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
                    .padding(end = spacing.extraSmall)
            )
        }
    }
}

@Composable
private fun Action(
    focusedItem: PluginCommandListItem?,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RaydroidTheme.spacing
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            spacing.medium,
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
                    onClick = onToggleActions,
                    color = if (showActions) {
                        PluginColor.Tertiary.toColor()
                    } else {
                        PluginColor.TertiaryContainer.toColor()
                    }
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        Modifier.size(PluginIconSize.ExtraSmall.toDp()),
                        tint = if (showActions) {
                            PluginColor.OnTertiary.toColor()
                        } else {
                            PluginColor.OnTertiaryContainer.toColor()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionsPanelOverlay(
    focusedItem: PluginCommandListItem?,
    visible: Boolean,
    onActionClick: (PluginCommandListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = focusedItem?.actions.orEmpty()
    val motion = RaydroidTheme.motionScheme.spec(RaydroidMotionToken.Fast)
    val groupedActions = remember(actions) {
        actions.groupBy { it.group }
            .entries.toList()
    }
    val popupShape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Medium)
    val popupRadius = RaydroidTheme.spacing.extraSmall
    AnimatedVisibility(
        visible = visible && actions.isNotEmpty(),
        enter = motion.popupEnter(TransformOrigin(1f, 1f)),
        exit = motion.popupExit(TransformOrigin(1f, 1f)),
        modifier = modifier
    ) {
        LazyColumn(
            Modifier
                .padding(popupRadius)
                .dropShadow(
                    popupShape,
                    shadow = Shadow(
                        color = RaydroidTheme.colorScheme.outline,
                        radius = RaydroidTheme.spacing.extraSmall
                    )
                )
                .clip(popupShape)
                .background(PluginColor.SurfaceBright.toColor())
                .heightIn(max = PopupHeight)
                .width(PopupWidth)
        ) {
            itemsIndexed(groupedActions) { index, (groupName, actionsList) ->
                Column(
                    Modifier.padding(RaydroidTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small)
                ) {
                    actionsList.forEach { action ->
                        ActionsPopupAction(action, onClick = { onActionClick(action) })
                    }
                }
                if (index != groupedActions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ActionsPopupAction(
    action: PluginCommandListAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RaydroidTheme.spacing
    val color = when (action.style) {
        PluginCommandListAction.Style.Default -> PluginColor.OnSurface
        PluginCommandListAction.Style.Destructive -> PluginColor.Error
    }
    Row(
        modifier
            .fillMaxWidth()
            .interactable {
                onClick()
            }
            .background(PluginColor.Surface.toColor())
            .padding(RaydroidTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconRenderer(
            PluginIconData(
                icon = action.icon ?: PluginIcon.Builtin("Help"),
                color = color,
                size = PluginIconSize.Small
            )
        )
        TextRenderer(
            data = PluginTextData(
                text = action.title,
                fontSize = PluginFontSize.Small,
                color = color
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Toast(toast: NotificationEvent.Toast, modifier: Modifier = Modifier) {
    Row(
        modifier
            .dropShadow(
                RaydroidTheme.shapes.medium,
                shadow = Shadow(
                    color = RaydroidTheme.colorScheme.outline,
                    radius = RaydroidTheme.spacing.extraSmall
                )
            )
            .clip(RaydroidTheme.shapes.medium)
            .background(RaydroidTheme.colorScheme.surfaceBright)
            .padding(RaydroidTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small)
    ) {
        CompositionLocalProvider(LocalContentColor provides PluginColor.OnTertiaryContainer.toColor()) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {
                when (toast.style) {
                    NotificationEvent.Toast.Style.Animated -> CircularWavyProgressIndicator(
                        Modifier.size(PluginIconSize.Small.toDp())
                    )

                    NotificationEvent.Toast.Style.Success -> Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        Modifier.size(PluginIconSize.Small.toDp())
                    )

                    NotificationEvent.Toast.Style.Failure -> Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        Modifier.size(PluginIconSize.Small.toDp())
                    )
                }
            }
            TextRenderer(PluginTextData(toast.message, fontSize = PluginFontSize.Small))
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
    val spacing = RaydroidTheme.spacing
    val shape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Small)
    Box(
        modifier
            .clip(shape)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .background(color)
            .padding(spacing.small)
    ) {
        content()
    }
}

private val PanelHeight = 80.dp
private val ToastHeight = 60.dp
private val PopupHeight = 180.dp
private val PopupWidth = 240.dp

@Preview
@Composable
private fun ActionPanelPreview() {
    RaydroidPreviewTheme {
        ToastsOverlay(
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
            )
        )
    }
}
