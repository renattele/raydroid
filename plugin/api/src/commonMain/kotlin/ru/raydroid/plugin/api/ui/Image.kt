package ru.raydroid.plugin.api.ui

import kotlinx.serialization.Serializable

@Serializable
sealed class Image {
    @Serializable
    data class Url(val url: String): Image()

    @Serializable
    data class Resource(val resource: String): Image()
}

@Serializable
data class ImageData(
    val image: Image,
    val contentDescription: String? = null,
    val width: Int? = null,
    val height: Int? = null
): RayNodeData()

@Ray
fun RayScope.Image(
    image: Image,
    contentDescription: String?,
    width: Int? = null,
    height: Int? = null
) {
    add(ImageData(
        image = image,
        contentDescription = contentDescription,
        width = width,
        height = height
    ))
}