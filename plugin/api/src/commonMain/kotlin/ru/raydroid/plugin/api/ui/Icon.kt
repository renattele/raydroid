package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
data class Icon(
    val value: String,
    val type: Type,
) {
    enum class Type {
        Url,
        Resource,
        Base64,
        Builtin,
    }

    companion object {
        fun Url(url: String) = Icon(url, Type.Url)

        fun Resource(resource: String) = Icon(resource, Type.Resource)

        fun Base64(base64: String) = Icon(base64, Type.Base64)

        fun Builtin(name: String) = Icon(name, Type.Builtin)
    }
}

enum class IconSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
}

@Serializable
data class IconData(
    val icon: Icon,
    val contentDescription: String? = null,
    val size: IconSize = IconSize.Medium,
) : RayNodeData()

@Ray
fun RayScope.Icon(
    icon: Icon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: IconSize = IconSize.Medium,
) {
    add(IconData(icon, contentDescription, size).withModifier(modifier(modifier)))
}
