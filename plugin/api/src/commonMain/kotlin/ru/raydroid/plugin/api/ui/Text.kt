package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.core.UiText

@Serializable
enum class FontSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}
@Serializable
data class TextData(
    val text: UiText,
    val fontSize: FontSize = FontSize.Medium,
    val color: Color = Color.OnSurface
): RayNodeData()

@Ray
fun RayScope.Text(text: UiText, fontSize: FontSize = FontSize.Medium, color: Color = Color.OnSurface) {
    add(TextData(text, fontSize, color))
}

