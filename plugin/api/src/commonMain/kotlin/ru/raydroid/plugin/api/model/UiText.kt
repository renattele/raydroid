package ru.raydroid.plugin.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UiText(
    val text: String,
    val type: Type,
) {
    enum class Type {
        Plain,
        Resource,
    }

    companion object {
        val Empty = UiText("", Type.Plain)

        fun Plain(text: String): UiText = UiText(text, Type.Plain)

        fun Resource(text: String): UiText = UiText(text, Type.Resource)
    }
}
