package ru.raydroid.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collectLatest
import ru.raydroid.core.designsystem.RaydroidTheme

internal object RInteractiveDefaults {
    const val FocusedScale = 1.02f
    const val DefaultScale = 1f

    fun targetScale(focused: Boolean): Float = if (focused) FocusedScale else DefaultScale
}

fun Modifier.rInteractable(
    enabled: Boolean = true,
    focused: Boolean = false,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember(focused) { mutableStateOf(focused) }
    LaunchedEffect(Unit) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> isFocused = true
                is FocusInteraction.Unfocus -> isFocused = false
            }
        }
    }
    val scale by animateFloatAsState(
        RInteractiveDefaults.targetScale(isFocused),
        animationSpec = RaydroidTheme.motionScheme.fast.floatSpec()
    )
    val color = RaydroidTheme.colorScheme.primary.copy(alpha = 0.1f)
    val shape = RaydroidTheme.shapes.medium
    combinedClickable(interactionSource, indication = null, enabled = enabled, onClick = onClick)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (isFocused) {
                drawRoundRect(color, cornerRadius = CornerRadius(shape.topStart.toPx(size, this)))
            }
        }
}
