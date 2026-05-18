package ru.raydroid.core.designsystem.component

import androidx.compose.runtime.Immutable
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

fun Modifier.rContextActionInactiveLayer(): Modifier = composed {
    val overlayState = LocalRContextActionOverlayState.current
    if (!overlayState.visible) {
        return@composed this
    }
    blur(18.dp).alpha(0.45f)
}

fun Modifier.rContextActionInactiveItem(sourceId: String?): Modifier = composed {
    val overlayState = LocalRContextActionOverlayState.current
    if (!overlayState.visible || sourceId == null || overlayState.activeSourceId == sourceId) {
        return@composed this
    }
    blur(12.dp).alpha(0.5f)
}
