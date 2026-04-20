package ru.raydroid.core.designsystem.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun RDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = Dp.Hairline,
    color: Color = Color.Unspecified,
) {
    if (color == Color.Unspecified) {
        HorizontalDivider(modifier = modifier, thickness = thickness)
    } else {
        HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
    }
}
