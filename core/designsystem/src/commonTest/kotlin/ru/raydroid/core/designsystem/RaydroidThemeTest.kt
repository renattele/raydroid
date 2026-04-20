package ru.raydroid.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.component.RInteractiveDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class RaydroidThemeTest {
    @Test
    fun `spacing tokens preserve current scale`() {
        val spacing = RaydroidSpacing()

        assertEquals(0.dp, spacing.value(RaydroidSpacingToken.Zero))
        assertEquals(Dp.Hairline, spacing.value(RaydroidSpacingToken.Minimal))
        assertEquals(1.dp, spacing.value(RaydroidSpacingToken.Border))
        assertEquals(4.dp, spacing.value(RaydroidSpacingToken.ExtraSmall))
        assertEquals(8.dp, spacing.value(RaydroidSpacingToken.Small))
        assertEquals(12.dp, spacing.value(RaydroidSpacingToken.Medium))
        assertEquals(16.dp, spacing.value(RaydroidSpacingToken.Large))
        assertEquals(28.dp, spacing.value(RaydroidSpacingToken.ExtraLarge))
    }

    @Test
    fun `shape tokens resolve to expected shapes`() {
        val shapes = RaydroidShapes.default()

        assertEquals(RectangleShape, shapes.shape(RaydroidShapeToken.None))
        assertEquals(RoundedCornerShape(4.dp), shapes.shape(RaydroidShapeToken.ExtraSmall))
        assertEquals(RoundedCornerShape(8.dp), shapes.shape(RaydroidShapeToken.Small))
        assertEquals(RoundedCornerShape(12.dp), shapes.shape(RaydroidShapeToken.Medium))
        assertEquals(RoundedCornerShape(16.dp), shapes.shape(RaydroidShapeToken.Large))
        assertEquals(RoundedCornerShape(28.dp), shapes.shape(RaydroidShapeToken.ExtraLarge))
        assertEquals(RoundedCornerShape(percent = 50), shapes.shape(RaydroidShapeToken.Full))
    }

    @Test
    fun `motion presets collapse scale when reduced motion is enabled`() {
        val normal = RaydroidMotionScheme.default(reduceMotion = false)
        val reduced = RaydroidMotionScheme.default(reduceMotion = true)

        assertEquals(180, normal.spec(RaydroidMotionToken.Default).durationMillis)
        assertEquals(0.03f, normal.spec(RaydroidMotionToken.Default).scaleDelta)
        assertEquals(false, normal.spec(RaydroidMotionToken.Default).usesExpressiveSpatialMotion)
        assertEquals(Spring.DampingRatioNoBouncy, normal.spec(RaydroidMotionToken.Default).dampingRatio)
        assertEquals(true, normal.spec(RaydroidMotionToken.Emphasized).usesExpressiveSpatialMotion)
        assertEquals(Spring.DampingRatioLowBouncy, normal.spec(RaydroidMotionToken.Emphasized).dampingRatio)
        assertEquals(120, reduced.spec(RaydroidMotionToken.Default).durationMillis)
        assertEquals(0f, reduced.spec(RaydroidMotionToken.Default).scaleDelta)
        assertEquals(false, reduced.spec(RaydroidMotionToken.Emphasized).usesExpressiveSpatialMotion)
    }

    @Test
    fun `interactive scale follows focused state`() {
        assertEquals(1f, RInteractiveDefaults.targetScale(focused = false))
        assertEquals(1.02f, RInteractiveDefaults.targetScale(focused = true))
    }
}
