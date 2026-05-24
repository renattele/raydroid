package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.model.UiText

@Serializable
enum class FontSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
}

@Serializable
enum class FontWeight {
    Normal,
    Bold,
}

@Serializable
data class TextData(
    val text: UiText,
    val fontSize: FontSize = FontSize.Medium,
    val fontWeight: FontWeight = FontWeight.Normal,
    val color: Color = Color.OnSurface,
) : RayNodeData()

@Ray
fun RayScope.Text(
    text: UiText,
    modifier: Modifier = Modifier,
    fontSize: FontSize = FontSize.Medium,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.OnSurface,
) {
    add(TextData(text, fontSize, fontWeight, color).withModifier(modifier(modifier)))
}
