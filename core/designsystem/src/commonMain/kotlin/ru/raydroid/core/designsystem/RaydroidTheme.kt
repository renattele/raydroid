package ru.raydroid.core.designsystem

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RaydroidSpacingToken {
    Zero,
    Minimal,
    Border,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

enum class RaydroidFontSizeToken {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

enum class RaydroidIconSizeToken {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

enum class RaydroidShapeToken {
    None,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
    Full
}

enum class RaydroidMotionToken {
    None,
    Fast,
    Default,
    Emphasized
}

@Immutable
data class RaydroidSpacing(
    val zero: Dp = 0.dp,
    val minimal: Dp = Dp.Hairline,
    val border: Dp = 1.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 28.dp,
) {
    fun value(token: RaydroidSpacingToken): Dp = when (token) {
        RaydroidSpacingToken.Zero -> zero
        RaydroidSpacingToken.Minimal -> minimal
        RaydroidSpacingToken.Border -> border
        RaydroidSpacingToken.ExtraSmall -> extraSmall
        RaydroidSpacingToken.Small -> small
        RaydroidSpacingToken.Medium -> medium
        RaydroidSpacingToken.Large -> large
        RaydroidSpacingToken.ExtraLarge -> extraLarge
    }
}

@Immutable
data class RaydroidTypographyScale(
    val extraSmall: TextUnit = 12.sp,
    val small: TextUnit = 16.sp,
    val medium: TextUnit = 20.sp,
    val large: TextUnit = 24.sp,
    val extraLarge: TextUnit = 28.sp,
) {
    fun fontSize(token: RaydroidFontSizeToken): TextUnit = when (token) {
        RaydroidFontSizeToken.ExtraSmall -> extraSmall
        RaydroidFontSizeToken.Small -> small
        RaydroidFontSizeToken.Medium -> medium
        RaydroidFontSizeToken.Large -> large
        RaydroidFontSizeToken.ExtraLarge -> extraLarge
    }
}

@Immutable
data class RaydroidIconSizes(
    val extraSmall: Dp = 16.dp,
    val small: Dp = 24.dp,
    val medium: Dp = 48.dp,
    val large: Dp = 72.dp,
    val extraLarge: Dp = 96.dp,
) {
    fun value(token: RaydroidIconSizeToken): Dp = when (token) {
        RaydroidIconSizeToken.ExtraSmall -> extraSmall
        RaydroidIconSizeToken.Small -> small
        RaydroidIconSizeToken.Medium -> medium
        RaydroidIconSizeToken.Large -> large
        RaydroidIconSizeToken.ExtraLarge -> extraLarge
    }
}

@Immutable
data class RaydroidShapes(
    val none: Shape,
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    val full: CornerBasedShape,
    val extraSmallCorner: Dp,
    val smallCorner: Dp,
    val mediumCorner: Dp,
    val largeCorner: Dp,
    val extraLargeCorner: Dp,
) {
    fun shape(token: RaydroidShapeToken): Shape = when (token) {
        RaydroidShapeToken.None -> none
        RaydroidShapeToken.ExtraSmall -> extraSmall
        RaydroidShapeToken.Small -> small
        RaydroidShapeToken.Medium -> medium
        RaydroidShapeToken.Large -> large
        RaydroidShapeToken.ExtraLarge -> extraLarge
        RaydroidShapeToken.Full -> full
    }

    fun attachedBottom(token: RaydroidShapeToken): Shape = when (token) {
        RaydroidShapeToken.None -> RectangleShape
        RaydroidShapeToken.ExtraSmall -> RoundedCornerShape(
            bottomStart = cornerSize(token),
            bottomEnd = cornerSize(token)
        )
        RaydroidShapeToken.Small -> RoundedCornerShape(
            bottomStart = cornerSize(token),
            bottomEnd = cornerSize(token)
        )
        RaydroidShapeToken.Medium -> RoundedCornerShape(
            bottomStart = cornerSize(token),
            bottomEnd = cornerSize(token)
        )
        RaydroidShapeToken.Large -> RoundedCornerShape(
            bottomStart = cornerSize(token),
            bottomEnd = cornerSize(token)
        )
        RaydroidShapeToken.ExtraLarge -> RoundedCornerShape(
            bottomStart = cornerSize(token),
            bottomEnd = cornerSize(token)
        )
        RaydroidShapeToken.Full -> RoundedCornerShape(bottomStartPercent = 50, bottomEndPercent = 50)
    }

    fun cornerSize(token: RaydroidShapeToken): Dp = when (token) {
        RaydroidShapeToken.None -> 0.dp
        RaydroidShapeToken.ExtraSmall -> extraSmallCorner
        RaydroidShapeToken.Small -> smallCorner
        RaydroidShapeToken.Medium -> mediumCorner
        RaydroidShapeToken.Large -> largeCorner
        RaydroidShapeToken.ExtraLarge -> extraLargeCorner
        RaydroidShapeToken.Full -> Dp.Unspecified
    }

    companion object {
        fun default(spacing: RaydroidSpacing = RaydroidSpacing()) = RaydroidShapes(
            none = RectangleShape,
            extraSmall = RoundedCornerShape(spacing.extraSmall),
            small = RoundedCornerShape(spacing.small),
            medium = RoundedCornerShape(spacing.medium),
            large = RoundedCornerShape(spacing.large),
            extraLarge = RoundedCornerShape(spacing.extraLarge),
            full = RoundedCornerShape(percent = 50),
            extraSmallCorner = spacing.extraSmall,
            smallCorner = spacing.small,
            mediumCorner = spacing.medium,
            largeCorner = spacing.large,
            extraLargeCorner = spacing.extraLarge
        )
    }
}

@Immutable
data class RaydroidMotionSpec(
    val durationMillis: Int,
    val scaleDelta: Float,
    val usesExpressiveSpatialMotion: Boolean,
    val dampingRatio: Float,
    val stiffness: Float,
    val effectsEasing: Easing,
    val enterEffectsEasing: Easing,
    val exitEffectsEasing: Easing,
) {
    fun floatSpec(): FiniteAnimationSpec<Float> = if (durationMillis == 0) {
        snap()
    } else if (usesExpressiveSpatialMotion) {
        spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    } else {
        tween(
            durationMillis = durationMillis,
            easing = effectsEasing
        )
    }

    fun dpSpec(): FiniteAnimationSpec<Dp> = if (durationMillis == 0) {
        snap()
    } else if (usesExpressiveSpatialMotion) {
        spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    } else {
        tween(
            durationMillis = durationMillis,
            easing = effectsEasing
        )
    }

    fun colorSpec(): FiniteAnimationSpec<Color> = if (durationMillis == 0) {
        snap()
    } else {
        tween(durationMillis = durationMillis, easing = effectsEasing)
    }

    fun popupEnter(transformOrigin: TransformOrigin = TransformOrigin.Center): EnterTransition {
        val fade = if (durationMillis == 0) {
            EnterTransition.None
        } else {
            fadeIn(animationSpec = tween(durationMillis = durationMillis, easing = enterEffectsEasing))
        }
        val scale = if (scaleDelta == 0f) {
            EnterTransition.None
        } else {
            scaleIn(
                initialScale = 1f - scaleDelta,
                animationSpec = floatSpec(),
                transformOrigin = transformOrigin
            )
        }
        return fade + scale
    }

    fun popupExit(transformOrigin: TransformOrigin = TransformOrigin.Center): ExitTransition {
        val fade = if (durationMillis == 0) {
            ExitTransition.None
        } else {
            fadeOut(animationSpec = tween(durationMillis = durationMillis, easing = exitEffectsEasing))
        }
        val scale = if (scaleDelta == 0f) {
            ExitTransition.None
        } else {
            scaleOut(
                targetScale = 1f - scaleDelta,
                animationSpec = floatSpec(),
                transformOrigin = transformOrigin
            )
        }
        return fade + scale
    }
}

@Immutable
data class RaydroidMotionScheme(
    val none: RaydroidMotionSpec,
    val fast: RaydroidMotionSpec,
    val default: RaydroidMotionSpec,
    val emphasized: RaydroidMotionSpec,
) {
    fun spec(token: RaydroidMotionToken): RaydroidMotionSpec = when (token) {
        RaydroidMotionToken.None -> none
        RaydroidMotionToken.Fast -> fast
        RaydroidMotionToken.Default -> default
        RaydroidMotionToken.Emphasized -> emphasized
    }

    companion object {
        fun default(reduceMotion: Boolean = false): RaydroidMotionScheme {
            val scaleDelta = if (reduceMotion) 0f else 0.04f
            return RaydroidMotionScheme(
                none = RaydroidMotionSpec(
                    durationMillis = 0,
                    scaleDelta = 0f,
                    usesExpressiveSpatialMotion = false,
                    dampingRatio = 1f,
                    stiffness = Spring.StiffnessHigh,
                    effectsEasing = FastOutSlowInEasing,
                    enterEffectsEasing = LinearOutSlowInEasing,
                    exitEffectsEasing = FastOutLinearInEasing
                ),
                fast = RaydroidMotionSpec(
                    durationMillis = if (reduceMotion) 80 else 120,
                    scaleDelta = scaleDelta / 2f,
                    usesExpressiveSpatialMotion = !reduceMotion,
                    dampingRatio = if (reduceMotion) 1f else Spring.DampingRatioMediumBouncy,
                    stiffness = if (reduceMotion) Spring.StiffnessMedium else Spring.StiffnessMediumLow,
                    effectsEasing = FastOutSlowInEasing,
                    enterEffectsEasing = LinearOutSlowInEasing,
                    exitEffectsEasing = FastOutLinearInEasing
                ),
                default = RaydroidMotionSpec(
                    durationMillis = if (reduceMotion) 120 else 180,
                    scaleDelta = scaleDelta * 0.75f,
                    usesExpressiveSpatialMotion = !reduceMotion,
                    dampingRatio = if (reduceMotion) 1f else Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    effectsEasing = FastOutSlowInEasing,
                    enterEffectsEasing = LinearOutSlowInEasing,
                    exitEffectsEasing = FastOutLinearInEasing
                ),
                emphasized = RaydroidMotionSpec(
                    durationMillis = if (reduceMotion) 160 else 260,
                    scaleDelta = scaleDelta * 1.5f,
                    usesExpressiveSpatialMotion = !reduceMotion,
                    dampingRatio = if (reduceMotion) 1f else Spring.DampingRatioLowBouncy,
                    stiffness = if (reduceMotion) Spring.StiffnessLow else Spring.StiffnessVeryLow,
                    effectsEasing = FastOutSlowInEasing,
                    enterEffectsEasing = LinearOutSlowInEasing,
                    exitEffectsEasing = FastOutLinearInEasing
                )
            )
        }
    }
}

private val LocalRaydroidSpacing = staticCompositionLocalOf { RaydroidSpacing() }
private val LocalRaydroidTypographyScale = staticCompositionLocalOf { RaydroidTypographyScale() }
private val LocalRaydroidIconSizes = staticCompositionLocalOf { RaydroidIconSizes() }
private val LocalRaydroidShapes = staticCompositionLocalOf { RaydroidShapes.default() }
private val LocalRaydroidMotionScheme = staticCompositionLocalOf { RaydroidMotionScheme.default() }

object RaydroidTheme {
    val colorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val spacing: RaydroidSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalRaydroidSpacing.current

    val typographyScale: RaydroidTypographyScale
        @Composable
        @ReadOnlyComposable
        get() = LocalRaydroidTypographyScale.current

    val iconSizes: RaydroidIconSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalRaydroidIconSizes.current

    val shapes: RaydroidShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalRaydroidShapes.current

    val motionScheme: RaydroidMotionScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalRaydroidMotionScheme.current
}

@Composable
fun RaydroidTheme(
    reduceMotion: Boolean = platformPrefersReducedMotion(),
    content: @Composable () -> Unit
) {
    val spacing = remember { RaydroidSpacing() }
    val typographyScale = remember { RaydroidTypographyScale() }
    val iconSizes = remember { RaydroidIconSizes() }
    val shapes = remember(spacing) { RaydroidShapes.default(spacing) }
    val motionScheme = remember(reduceMotion) { RaydroidMotionScheme.default(reduceMotion) }
    val materialShapes = remember(shapes) {
        Shapes(
            small = shapes.small,
            medium = shapes.medium,
            large = shapes.large
        )
    }
    MaterialTheme(
        shapes = materialShapes
    ) {
        CompositionLocalProvider(
            LocalRaydroidSpacing provides spacing,
            LocalRaydroidTypographyScale provides typographyScale,
            LocalRaydroidIconSizes provides iconSizes,
            LocalRaydroidShapes provides shapes,
            LocalRaydroidMotionScheme provides motionScheme,
            content = content
        )
    }
}

internal expect fun platformPrefersReducedMotion(): Boolean
