package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class Icon {
    @Serializable
    data class App(val appId: String): Icon()

    @Serializable
    data class Url(val url: String): Icon()

    @Serializable
    data class Resource(val resource: String): Icon()

    @Serializable
    data class Base64(val value: String): Icon()
}

enum class IconSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

@Serializable
data class IconData(
    val icon: Icon,
    val contentDescription: String? = null,
    val size: IconSize = IconSize.Medium
): RayNodeData()

@Ray
fun RayScope.Icon(icon: Icon, contentDescription: String?, size: IconSize = IconSize.Medium) {
    add(IconData(icon, contentDescription, size))
}