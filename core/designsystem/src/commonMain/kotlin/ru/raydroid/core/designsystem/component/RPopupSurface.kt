package ru.raydroid.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import ru.raydroid.core.designsystem.RaydroidTheme

@Composable
fun RPopupSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RaydroidTheme.shapes.medium,
    color: Color = RaydroidTheme.colorScheme.surfaceBright,
    shadowColor: Color = RaydroidTheme.colorScheme.outline,
    shadowRadius: Dp = RaydroidTheme.spacing.extraSmall,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .dropShadow(
                shape,
                shadow = Shadow(
                    color = shadowColor,
                    radius = shadowRadius
                )
            )
            .clip(shape)
            .background(color),
        content = content
    )
}
