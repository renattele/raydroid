package ru.raydroid.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collectLatest
import ru.raydroid.core.designsystem.RaydroidTheme

internal object RInteractiveDefaults {
    const val PressedScale = 1.02f
    const val FocusedScale = 1.02f
    const val ContextMenuScale = 1.06f
    const val DefaultScale = 1f

    fun targetScale(
        focused: Boolean,
        pressed: Boolean,
        contextMenuActive: Boolean
    ): Float = when {
        contextMenuActive -> ContextMenuScale
        pressed -> PressedScale
        focused -> FocusedScale
        else -> DefaultScale
    }
}

fun Modifier.rInteractable(
    enabled: Boolean = true,
    focused: Boolean = false,
    contextMenuSourceId: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val overlayState = LocalRContextActionOverlayState.current
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember(focused) { mutableStateOf(focused) }
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> isFocused = true
                is FocusInteraction.Unfocus -> isFocused = false
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release,
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }
    val contextMenuActive = overlayState.visible && overlayState.activeSourceId == contextMenuSourceId
    val scale by animateFloatAsState(
        RInteractiveDefaults.targetScale(
            focused = isFocused,
            pressed = isPressed,
            contextMenuActive = contextMenuActive
        ),
        animationSpec = RaydroidTheme.motionScheme.fast.floatSpec()
    )
    val color = if (isFocused || isPressed || contextMenuActive) {
        RaydroidTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    } else {
        Color.Transparent
    }
    val shape = RaydroidTheme.shapes.medium
    rContextActionAnchor(contextMenuSourceId)
        .rContextActionInactiveItem(contextMenuSourceId)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            if (isFocused) {
                drawRoundRect(color, cornerRadius = CornerRadius(shape.topStart.toPx(size, this)))
            }
            drawContent()
        }
}
