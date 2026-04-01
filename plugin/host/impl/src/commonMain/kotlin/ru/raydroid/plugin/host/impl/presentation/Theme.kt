package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginSpacing

@Composable
fun PluginColor.toColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (this) {
        PluginColor.Primary -> colorScheme.primary
        PluginColor.PrimaryContainer -> colorScheme.primaryContainer
        PluginColor.OnPrimary -> colorScheme.onPrimary
        PluginColor.OnPrimaryContainer -> colorScheme.onPrimaryContainer
        PluginColor.Secondary -> colorScheme.secondary
        PluginColor.OnSecondary -> colorScheme.onSecondary
        PluginColor.SecondaryContainer -> colorScheme.secondaryContainer
        PluginColor.OnSecondaryContainer -> colorScheme.onSecondaryContainer
        PluginColor.Tertiary -> colorScheme.tertiary
        PluginColor.OnTertiary -> colorScheme.onTertiary
        PluginColor.TertiaryContainer -> colorScheme.tertiaryContainer
        PluginColor.OnTertiaryContainer -> colorScheme.onTertiaryContainer
        PluginColor.Error -> colorScheme.error
        PluginColor.ErrorContainer -> colorScheme.errorContainer
        PluginColor.OnError -> colorScheme.onError
        PluginColor.OnErrorContainer -> colorScheme.onErrorContainer
        PluginColor.PrimaryFixed -> colorScheme.primaryFixed
        PluginColor.PrimaryFixedDim -> colorScheme.primaryFixedDim
        PluginColor.OnPrimaryFixed -> colorScheme.onPrimaryFixed
        PluginColor.OnPrimaryFixedVariant -> colorScheme.onPrimaryFixedVariant
        PluginColor.SecondaryFixed -> colorScheme.secondaryFixed
        PluginColor.SecondaryFixedDim -> colorScheme.secondaryFixedDim
        PluginColor.OnSecondaryFixed -> colorScheme.onSecondaryFixed
        PluginColor.OnSecondaryFixedVariant -> colorScheme.onSecondaryFixedVariant
        PluginColor.TertiaryFixed -> colorScheme.tertiaryFixed
        PluginColor.TertiaryFixedDim -> colorScheme.tertiaryFixedDim
        PluginColor.OnTertiaryFixed -> colorScheme.onTertiaryFixed
        PluginColor.OnTertiaryFixedVariant -> colorScheme.onTertiaryFixedVariant
        PluginColor.SurfaceDim -> colorScheme.surfaceDim
        PluginColor.Surface -> colorScheme.surface
        PluginColor.SurfaceBright -> colorScheme.surfaceBright
        PluginColor.SurfaceContainerLowest -> colorScheme.surfaceContainerLowest
        PluginColor.SurfaceContainerLow -> colorScheme.surfaceContainerLow
        PluginColor.SurfaceContainer -> colorScheme.surfaceContainer
        PluginColor.SurfaceContainerHigh -> colorScheme.surfaceContainerHigh
        PluginColor.SurfaceContainerHighest -> colorScheme.surfaceContainerHighest
        PluginColor.OnSurface -> colorScheme.onSurface
        PluginColor.OnSurfaceVariant -> colorScheme.onSurfaceVariant
        PluginColor.Outline -> colorScheme.outline
        PluginColor.OutlineVariant -> colorScheme.outlineVariant
        PluginColor.InverseSurface -> colorScheme.inverseSurface
        PluginColor.InverseOnSurface -> colorScheme.inverseOnSurface
        PluginColor.InversePrimary -> colorScheme.inversePrimary
        PluginColor.Scrim -> colorScheme.scrim
        PluginColor.Transparent -> Color.Transparent
    }
}

fun PluginSpacing.toDp(): Dp = when (this) {
    PluginSpacing.Zero -> 0.dp
    PluginSpacing.ExtraSmall -> 4.dp
    PluginSpacing.Small -> 8.dp
    PluginSpacing.Medium -> 12.dp
    PluginSpacing.Large -> 16.dp
    PluginSpacing.ExtraLarge -> 28.dp
    PluginSpacing.Minimal -> Dp.Hairline
    PluginSpacing.Border -> 1.dp
}

fun PluginFontSize.toTextUnit(): TextUnit = when (this) {
    PluginFontSize.ExtraSmall -> 12.sp
    PluginFontSize.Small -> 16.sp
    PluginFontSize.Medium -> 20.sp
    PluginFontSize.Large -> 24.sp
    PluginFontSize.ExtraLarge -> 28.sp
}

fun PluginIconSize.toDp(): Dp = when (this) {
    PluginIconSize.ExtraSmall -> 16.dp
    PluginIconSize.Small -> 24.dp
    PluginIconSize.Medium -> 48.dp
    PluginIconSize.Large -> 72.dp
    PluginIconSize.ExtraLarge -> 96.dp
}

@Composable
fun RaydroidPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        PreviewResourceResolverProvider {
            content()
        }
    }
}
