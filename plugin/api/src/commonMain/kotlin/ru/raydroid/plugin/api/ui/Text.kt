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
    val fontSize: FontSize
): RayNodeData()

@Ray
fun RayScope.Text(text: UiText, fontSize: FontSize = FontSize.Medium) {
    add(TextData(text, fontSize))
}

