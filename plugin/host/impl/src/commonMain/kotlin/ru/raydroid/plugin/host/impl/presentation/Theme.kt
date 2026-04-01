package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.IconSize
import ru.raydroid.plugin.api.ui.Spacing

interface ThemeResolver {
    fun resolveSpacing(spacing: Spacing): Dp
    fun resolveTextSize(fontSize: FontSize): TextUnit
    fun resolveIconSize(iconSize: IconSize): Dp
    fun resolveColor(color: Color): androidx.compose.ui.graphics.Color
}

@Composable
fun ThemeResolver.color(color: Color) = remember(color) {
    resolveColor(color)
}

@Composable
fun ThemeResolver.fontSize(fontSize: FontSize) = remember(fontSize) {
    resolveTextSize(fontSize)
}

@Composable
fun ThemeResolver.spacing(spacing: Spacing) = remember(spacing) {
    resolveSpacing(spacing)
}

@Composable
fun ThemeResolver.iconSize(iconSize: IconSize) = remember(iconSize) {
    resolveIconSize(iconSize)
}

internal val LocalThemeResolver = staticCompositionLocalOf<ThemeResolver> { error("No ThemeResolver is provided") }

internal class ThemeResolverImpl(
    private val colorScheme: ColorScheme
): ThemeResolver {
    override fun resolveSpacing(spacing: Spacing): Dp = when (spacing) {
        Spacing.Zero -> 0.dp
        Spacing.ExtraSmall -> 4.dp
        Spacing.Small -> 8.dp
        Spacing.Medium -> 12.dp
        Spacing.Large -> 16.dp
        Spacing.ExtraLarge -> 28.dp
        Spacing.Minimal -> Dp.Hairline
        Spacing.Border -> 1.dp
    }

    override fun resolveTextSize(fontSize: FontSize): TextUnit = when (fontSize) {
        FontSize.ExtraSmall -> 12.sp
        FontSize.Small -> 16.sp
        FontSize.Medium -> 20.sp
        FontSize.Large -> 24.sp
        FontSize.ExtraLarge -> 28.sp
    }

    override fun resolveIconSize(iconSize: IconSize): Dp = when (iconSize) {
        IconSize.ExtraSmall -> 16.dp
        IconSize.Small -> 24.dp
        IconSize.Medium -> 48.dp
        IconSize.Large -> 72.dp
        IconSize.ExtraLarge -> 96.dp
    }

    override fun resolveColor(color: Color): androidx.compose.ui.graphics.Color = when (color) {
        Color.Primary -> colorScheme.primary
        Color.PrimaryContainer -> colorScheme.primaryContainer
        Color.OnPrimary -> colorScheme.onPrimary
        Color.OnPrimaryContainer -> colorScheme.onPrimaryContainer
        Color.Secondary -> colorScheme.secondary
        Color.OnSecondary -> colorScheme.onSecondary
        Color.SecondaryContainer -> colorScheme.secondaryContainer
        Color.OnSecondaryContainer -> colorScheme.onSecondaryContainer
        Color.Tertiary -> colorScheme.tertiary
        Color.OnTertiary -> colorScheme.onTertiary
        Color.TertiaryContainer -> colorScheme.tertiaryContainer
        Color.OnTertiaryContainer -> colorScheme.onTertiaryContainer
        Color.Error -> colorScheme.error
        Color.ErrorContainer -> colorScheme.errorContainer
        Color.OnError -> colorScheme.onError
        Color.OnErrorContainer -> colorScheme.onErrorContainer
        Color.PrimaryFixed -> colorScheme.primaryFixed
        Color.PrimaryFixedDim -> colorScheme.primaryFixedDim
        Color.OnPrimaryFixed -> colorScheme.onPrimaryFixed
        Color.OnPrimaryFixedVariant -> colorScheme.onPrimaryFixedVariant
        Color.SecondaryFixed -> colorScheme.secondaryFixed
        Color.SecondaryFixedDim -> colorScheme.secondaryFixedDim
        Color.OnSecondaryFixed -> colorScheme.onSecondaryFixed
        Color.OnSecondaryFixedVariant -> colorScheme.onSecondaryFixedVariant
        Color.TertiaryFixed -> colorScheme.tertiaryFixed
        Color.TertiaryFixedDim -> colorScheme.tertiaryFixedDim
        Color.OnTertiaryFixed -> colorScheme.onTertiaryFixed
        Color.OnTertiaryFixedVariant -> colorScheme.onTertiaryFixedVariant
        Color.SurfaceDim -> colorScheme.surfaceDim
        Color.Surface -> colorScheme.surface
        Color.SurfaceBright -> colorScheme.surfaceBright
        Color.SurfaceContainerLowest -> colorScheme.surfaceContainerLowest
        Color.SurfaceContainerLow -> colorScheme.surfaceContainerLow
        Color.SurfaceContainer -> colorScheme.surfaceContainer
        Color.SurfaceContainerHigh -> colorScheme.surfaceContainerHigh
        Color.SurfaceContainerHighest -> colorScheme.surfaceContainerHighest
        Color.OnSurface -> colorScheme.onSurface
        Color.OnSurfaceVariant -> colorScheme.onSurfaceVariant
        Color.Outline -> colorScheme.outline
        Color.OutlineVariant -> colorScheme.outlineVariant
        Color.InverseSurface -> colorScheme.inverseSurface
        Color.InverseOnSurface -> colorScheme.inverseOnSurface
        Color.InversePrimary -> colorScheme.inversePrimary
        Color.Scrim -> colorScheme.scrim
        Color.Transparent -> androidx.compose.ui.graphics.Color.Transparent
    }
}

@Composable
fun RaydroidPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        ThemeProvider {
            PreviewResourceResolverProvider {
                content()
            }
        }
    }
}