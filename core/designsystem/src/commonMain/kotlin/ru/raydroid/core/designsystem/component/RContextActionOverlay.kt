package ru.raydroid.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.zIndex
import ru.raydroid.core.designsystem.RaydroidMotionToken
import ru.raydroid.core.designsystem.RaydroidTheme

@Immutable
data class RContextActionOverlayState(
    val activeSourceId: String? = null,
    val visible: Boolean = false,
    val registerAnchor: (String, Rect) -> Unit = { _, _ -> },
    val unregisterAnchor: (String) -> Unit = {}
)

val LocalRContextActionOverlayState = staticCompositionLocalOf {
    RContextActionOverlayState()
}

fun Modifier.rContextActionAnchor(sourceId: String?): Modifier = composed {
    if (sourceId == null) {
        return@composed this
    }
    val overlayState = LocalRContextActionOverlayState.current
    DisposableEffect(overlayState, sourceId) {
        onDispose {
            overlayState.unregisterAnchor(sourceId)
        }
    }
    onGloballyPositioned { coordinates ->
        overlayState.registerAnchor(sourceId, coordinates.boundsInWindow())
    }.zIndex(
        if (overlayState.visible && overlayState.activeSourceId == sourceId) {
            4f
        } else {
            0f
        }
    )
}

fun Modifier.rContextActionInactiveLayer(blur: Boolean = true): Modifier = composed {
    val overlayState = LocalRContextActionOverlayState.current
    val motion = RaydroidTheme.motionScheme.spec(RaydroidMotionToken.Fast)
    val blurRadius by animateDpAsState(
        targetValue = if (overlayState.visible && blur) 18.dp else 0.dp,
        animationSpec = motion.dpSpec(),
        label = "rContextActionInactiveLayerBlur"
    )
    val alphaValue by animateFloatAsState(
        targetValue = if (overlayState.visible) 0.45f else 1f,
        animationSpec = motion.floatSpec(),
        label = "rContextActionInactiveLayerAlpha"
    )
    blur(blurRadius).alpha(alphaValue)
}

fun Modifier.rContextActionInactiveItem(sourceId: String?): Modifier = composed {
    val overlayState = LocalRContextActionOverlayState.current
    val isInactive = overlayState.visible &&
        sourceId != null &&
        overlayState.activeSourceId != sourceId
    val motion = RaydroidTheme.motionScheme.spec(RaydroidMotionToken.Fast)
    val blurRadius by animateDpAsState(
        targetValue = if (isInactive) 12.dp else 0.dp,
        animationSpec = motion.dpSpec(),
        label = "rContextActionInactiveItemBlur"
    )
    val alphaValue by animateFloatAsState(
        targetValue = if (isInactive) 0.5f else 1f,
        animationSpec = motion.floatSpec(),
        label = "rContextActionInactiveItemAlpha"
    )
    blur(blurRadius).alpha(alphaValue)
}
