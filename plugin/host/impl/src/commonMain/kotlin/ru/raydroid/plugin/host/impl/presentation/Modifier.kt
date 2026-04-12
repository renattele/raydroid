package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collectLatest
import ru.raydroid.core.designsystem.RaydroidTheme

// TODO: Replace with Modifier.Node
fun Modifier.interactable(
    enabled: Boolean = true,
    focused: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = remember { ripple() }
    var focused by remember(focused) { mutableStateOf(focused) }
    LaunchedEffect(Unit) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> focused = true
                is FocusInteraction.Unfocus -> focused = false
            }
            println(interaction)
        }
    }
    val scale by animateFloatAsState(
        if (focused) 1.02f else 1f,
        animationSpec = RaydroidTheme.motionScheme.fast.floatSpec()
    )
    val color = RaydroidTheme.colorScheme.primary.copy(alpha = 0.1f)
    val shape = RaydroidTheme.shapes.medium
    combinedClickable(interactionSource, indication, enabled = enabled, onClick = onClick)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (focused) {
                drawRoundRect(color, cornerRadius = CornerRadius(shape.topStart.toPx(size, this)))
            }
        }
}