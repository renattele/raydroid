package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
enum class ShapeToken {
    None,
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
    Full,
}

@Serializable
enum class MotionToken {
    None,
    Fast,
    Default,
    Emphasized,
}
