package ru.raydroid.plugin.api.core

import kotlinx.serialization.Serializable

@Serializable
data class UiText(
    val text: String,
    val type: Type
) {
    enum class Type {
        Plain,
        Resource
    }

    companion object {
        val Empty = UiText("", Type.Plain)
        fun Plain(text: String): UiText {
            return UiText(text, Type.Plain)
        }
        fun Resource(text: String): UiText {
            return UiText(text, Type.Resource)
        }
    }
}