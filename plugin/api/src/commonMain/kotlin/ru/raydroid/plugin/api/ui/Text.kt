package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

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
    val text: String,
    val fontSize: FontSize
): RayNodeData()

@Ray
fun RayScope.Text(text: String, fontSize: FontSize = FontSize.Medium) {
    add(TextData(text, fontSize))
}

