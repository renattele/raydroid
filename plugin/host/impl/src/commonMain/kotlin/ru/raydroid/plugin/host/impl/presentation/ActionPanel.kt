package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.RaydroidMotionToken
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RDivider
import ru.raydroid.core.designsystem.component.RIcon
import ru.raydroid.core.designsystem.component.RKeyHint
import ru.raydroid.core.designsystem.component.RLoadingIndicator
import ru.raydroid.core.designsystem.component.RPopupSurface
import ru.raydroid.core.designsystem.component.rInteractable
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.NotificationEvent
import ru.raydroid.plugin.host.api.ui.ActionPanelActionUi
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginIconData
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginTextData
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.math.pow

@Composable
fun ActionPanel(
    actions: List<ActionPanelActionUi>,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    showPrimaryHint: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = RaydroidTheme.spacing
    Row(
        modifier
            .padding(horizontal = spacing.medium)
    ) {
        Action(
            actions = actions,
            showActions = showActions,
            onToggleActions = onToggleActions,
            showPrimaryHint = showPrimaryHint,
            onPrimaryAction = onPrimaryAction
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
    actions: List<ActionPanelActionUi>,
    showActions: Boolean,
    onToggleActions: () -> Unit,
    showPrimaryHint: Boolean,
    onPrimaryAction: (() -> Unit)?,
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
        val primaryAction = remember(actions) {
            actions.find { it.primary } ?: actions.firstOrNull()
        }
        if (primaryAction != null) {
            if (showPrimaryHint) {
                TextRenderer(
                    PluginTextData(
                        text = primaryAction.title,
                        color = PluginColor.OnSurfaceVariant,
                        fontSize = PluginFontSize.ExtraSmall
                    )
                )
                RKeyHint(onClick = onPrimaryAction) {
                    RIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = null,
                        modifier = Modifier.size(PluginIconSize.ExtraSmall.toDp()),
                        tint = PluginColor.OnPrimaryContainer.toColor()
                    )
                }
            }
            RKeyHint(
                onClick = onToggleActions,
                color = if (showActions) {
                    PluginColor.Tertiary.toColor()
                } else {
                    PluginColor.TertiaryContainer.toColor()
                }
            ) {
                RIcon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(PluginIconSize.ExtraSmall.toDp()),
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

@Composable
fun ActionsPanelOverlay(
    actions: List<ActionPanelActionUi>,
    visible: Boolean,
    onActionClick: (ActionPanelActionUi) -> Unit,
    modifier: Modifier = Modifier
) {
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
        RPopupSurface(
            modifier = Modifier
                .padding(popupRadius)
                .heightIn(max = PopupHeight)
                .width(PopupWidth),
            shape = popupShape,
            color = RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
        ) {
            LazyColumn {
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
                        RDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsPopupAction(
    action: ActionPanelActionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RaydroidTheme.spacing
    val color = if (action.destructive) {
        PluginColor.Error
    } else {
        PluginColor.OnSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .rInteractable(enabled = action.enabled) {
                onClick()
            }
            .background(Color.Transparent)
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

@Composable
private fun Toast(toast: NotificationEvent.Toast, modifier: Modifier = Modifier) {
    RPopupSurface(
        modifier = modifier.width(PopupWidth),
        shape = RaydroidTheme.shapes.medium,
        color = RaydroidTheme.colorScheme.surfaceBright
    ) {
        Row(
            Modifier.padding(RaydroidTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RaydroidTheme.spacing.small)
        ) {
            CompositionLocalProvider(LocalContentColor provides PluginColor.OnTertiaryContainer.toColor()) {
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    when (toast.style) {
                        NotificationEvent.Toast.Style.Animated -> RLoadingIndicator(
                            Modifier.size(PluginIconSize.Small.toDp())
                        )

                        NotificationEvent.Toast.Style.Success -> RIcon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(PluginIconSize.Small.toDp())
                        )

                        NotificationEvent.Toast.Style.Failure -> RIcon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(PluginIconSize.Small.toDp())
                        )
                    }
                }
                TextRenderer(PluginTextData(toast.message, fontSize = PluginFontSize.Small))
            }
        }
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
                    toastId = "toast-1",
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message"),
                        style = NotificationEvent.Toast.Style.Animated,
                        autoDismissMillis = null
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid,
                    toastId = "toast-2",
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message2"),
                        style = NotificationEvent.Toast.Style.Success,
                        autoDismissMillis = 5_000L
                    )
                ),
                NotificationEvent.ShowToast(
                    PluginId.Invalid,
                    toastId = "toast-3",
                    toast = NotificationEvent.Toast(
                        message = PluginUiText.Plain("Message3"),
                        style = NotificationEvent.Toast.Style.Success,
                        autoDismissMillis = 5_000L
                    )
                )
            )
        )
    }
}
