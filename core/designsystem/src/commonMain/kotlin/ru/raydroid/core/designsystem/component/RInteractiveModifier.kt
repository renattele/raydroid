package ru.raydroid.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    val interactionFocused by interactionSource.collectIsFocusedAsState()
    val interactionPressed by interactionSource.collectIsPressedAsState()
    val isFocused = focused || interactionFocused
    val isPressed = interactionPressed
    val contextMenuActive = overlayState.visible && overlayState.activeSourceId == contextMenuSourceId
    val motion = RaydroidTheme.motionScheme.fast
    val density = LocalDensity.current
    val scale by animateFloatAsState(
        RInteractiveDefaults.targetScale(
            focused = isFocused,
            pressed = isPressed,
            contextMenuActive = contextMenuActive
        ),
        animationSpec = motion.floatSpec()
    )
    val overflowPadding by animateDpAsState(
        targetValue = if (isPressed || contextMenuActive) 8.dp else 0.dp,
        animationSpec = motion.dpSpec()
    )
    val overflowPaddingPx = with(density) { overflowPadding.roundToPx() }
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
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val paddingPx = overflowPaddingPx
            val width = placeable.width + paddingPx * 2
            val height = placeable.height + paddingPx * 2
            layout(width, height) {
                placeable.placeRelative(paddingPx, paddingPx)
            }
        }
        .offset {
            IntOffset(-overflowPaddingPx, -overflowPaddingPx)
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            if (color.alpha > 0f) {
                drawRoundRect(color, cornerRadius = CornerRadius(shape.topStart.toPx(size, this)))
            }
            drawContent()
        }
}
