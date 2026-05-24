package ru.raydroid.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidTheme

@Composable
fun RKeyHint(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = RaydroidTheme.colorScheme.primaryContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    val spacing = RaydroidTheme.spacing
    val shape = RaydroidTheme.shapes.shape(RaydroidShapeToken.Small)
    Box(
        modifier
            .clip(shape)
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }.background(color)
            .padding(spacing.small),
        content = content,
    )
}
