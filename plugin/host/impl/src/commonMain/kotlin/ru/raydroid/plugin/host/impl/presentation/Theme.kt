package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import ru.raydroid.core.designsystem.RaydroidFontSizeToken
import ru.raydroid.core.designsystem.RaydroidIconSizeToken
import ru.raydroid.core.designsystem.RaydroidMotionSpec
import ru.raydroid.core.designsystem.RaydroidMotionToken
import ru.raydroid.core.designsystem.RaydroidShapeToken
import ru.raydroid.core.designsystem.RaydroidSpacingToken
import ru.raydroid.plugin.host.api.ui.PluginColor
import ru.raydroid.plugin.host.api.ui.PluginFontSize
import ru.raydroid.plugin.host.api.ui.PluginIconSize
import ru.raydroid.plugin.host.api.ui.PluginMotionToken
import ru.raydroid.plugin.host.api.ui.PluginShapeToken
import ru.raydroid.plugin.host.api.ui.PluginSpacing

@Composable
fun PluginColor.toColor(): Color {
    val colorScheme = ru.raydroid.core.designsystem.RaydroidTheme.colorScheme
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

fun PluginSpacing.toSpacingToken(): RaydroidSpacingToken =
    when (this) {
        PluginSpacing.Zero -> RaydroidSpacingToken.Zero
        PluginSpacing.Minimal -> RaydroidSpacingToken.Minimal
        PluginSpacing.Border -> RaydroidSpacingToken.Border
        PluginSpacing.ExtraSmall -> RaydroidSpacingToken.ExtraSmall
        PluginSpacing.Small -> RaydroidSpacingToken.Small
        PluginSpacing.Medium -> RaydroidSpacingToken.Medium
        PluginSpacing.Large -> RaydroidSpacingToken.Large
        PluginSpacing.ExtraLarge -> RaydroidSpacingToken.ExtraLarge
    }

@Composable
fun PluginSpacing.toDp(): Dp =
    ru.raydroid.core.designsystem.RaydroidTheme.spacing
        .value(toSpacingToken())

private fun PluginFontSize.toFontSizeToken(): RaydroidFontSizeToken =
    when (this) {
        PluginFontSize.ExtraSmall -> RaydroidFontSizeToken.ExtraSmall
        PluginFontSize.Small -> RaydroidFontSizeToken.Small
        PluginFontSize.Medium -> RaydroidFontSizeToken.Medium
        PluginFontSize.Large -> RaydroidFontSizeToken.Large
        PluginFontSize.ExtraLarge -> RaydroidFontSizeToken.ExtraLarge
    }

@Composable
fun PluginFontSize.toTextUnit(): TextUnit =
    ru.raydroid.core.designsystem.RaydroidTheme.typographyScale
        .fontSize(toFontSizeToken())

private fun PluginIconSize.toIconSizeToken(): RaydroidIconSizeToken =
    when (this) {
        PluginIconSize.ExtraSmall -> RaydroidIconSizeToken.ExtraSmall
        PluginIconSize.Small -> RaydroidIconSizeToken.Small
        PluginIconSize.Medium -> RaydroidIconSizeToken.Medium
        PluginIconSize.Large -> RaydroidIconSizeToken.Large
        PluginIconSize.ExtraLarge -> RaydroidIconSizeToken.ExtraLarge
    }

@Composable
fun PluginIconSize.toDp(): Dp =
    ru.raydroid.core.designsystem.RaydroidTheme.iconSizes
        .value(toIconSizeToken())

fun PluginShapeToken.toRaydroidShapeToken(): RaydroidShapeToken =
    when (this) {
        PluginShapeToken.None -> RaydroidShapeToken.None
        PluginShapeToken.ExtraSmall -> RaydroidShapeToken.ExtraSmall
        PluginShapeToken.Small -> RaydroidShapeToken.Small
        PluginShapeToken.Medium -> RaydroidShapeToken.Medium
        PluginShapeToken.Large -> RaydroidShapeToken.Large
        PluginShapeToken.ExtraLarge -> RaydroidShapeToken.ExtraLarge
        PluginShapeToken.Full -> RaydroidShapeToken.Full
    }

@Composable
fun PluginShapeToken.toShape(): Shape =
    ru.raydroid.core.designsystem.RaydroidTheme.shapes
        .shape(toRaydroidShapeToken())

fun PluginMotionToken.toRaydroidMotionToken(): RaydroidMotionToken =
    when (this) {
        PluginMotionToken.None -> RaydroidMotionToken.None
        PluginMotionToken.Fast -> RaydroidMotionToken.Fast
        PluginMotionToken.Default -> RaydroidMotionToken.Default
        PluginMotionToken.Emphasized -> RaydroidMotionToken.Emphasized
    }

@Composable
fun PluginMotionToken.toMotionSpec(): RaydroidMotionSpec =
    ru.raydroid.core.designsystem.RaydroidTheme.motionScheme
        .spec(toRaydroidMotionToken())

@Composable
fun RaydroidPreviewTheme(content: @Composable () -> Unit) {
    ru.raydroid.core.designsystem.RaydroidTheme {
        PreviewResourceResolverProvider {
            content()
        }
    }
}
