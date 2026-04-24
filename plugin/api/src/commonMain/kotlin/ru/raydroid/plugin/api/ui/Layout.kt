package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
enum class Spacing {
    Zero,
    Minimal,
    Border,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

@Serializable
enum class Alignment {
    Start,
    Center,
    End
}

@Serializable
enum class BoxAlignment {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}


@Serializable
enum class Orientation {
    Vertical,
    Horizontal
}


@Serializable
enum class Arrangement {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

@Serializable
data class BoxData(
    val alignment: BoxAlignment,
    val shape: ShapeToken = ShapeToken.None,
    val children: List<RayNodeData>
): RayNodeData()

@Ray
fun RayScope.Box(
    modifier: Modifier = Modifier,
    alignment: BoxAlignment = BoxAlignment.TopStart,
    shape: ShapeToken = ShapeToken.None,
    content: RayScope.() -> Unit
) {
    val children = fork(content)
    add(BoxData(alignment, shape, children).withModifier(modifier(modifier)))
}

@Serializable
data class OrientedBoxData(
    val orientation: Orientation,
    val alignment: Alignment,
    val arrangement: Arrangement,
    val spacing: Spacing,
    val shape: ShapeToken = ShapeToken.None,
    val children: List<RayNodeData>
): RayNodeData()

@Ray
fun RayScope.Row(
    modifier: Modifier = Modifier,
    spacing: Spacing = Spacing.Zero,
    alignment: Alignment = Alignment.Start,
    arrangement: Arrangement = Arrangement.Start,
    shape: ShapeToken = ShapeToken.None,
    content: RayScope.() -> Unit
) {
    val children = fork(content)
    add(OrientedBoxData(Orientation.Horizontal, alignment, arrangement, spacing, shape, children).withModifier(modifier(modifier)))
}

@Ray
fun RayScope.Column(
    modifier: Modifier = Modifier,
    spacing: Spacing = Spacing.Zero,
    alignment: Alignment = Alignment.Start,
    arrangement: Arrangement = Arrangement.Start,
    shape: ShapeToken = ShapeToken.None,
    content: RayScope.() -> Unit
) {
    val children = fork(content)
    add(OrientedBoxData(Orientation.Vertical, alignment, arrangement, spacing, shape, children).withModifier(modifier(modifier)))
}
