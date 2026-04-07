package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginUiText

sealed interface ResolvedPluginIcon {
    data class ImageModel(val model: Any) : ResolvedPluginIcon
    data class Vector(val imageVector: ImageVector) : ResolvedPluginIcon
}

interface ResourceResolver {
    fun resolveText(text: PluginUiText): String
    fun resolveIcon(icon: PluginIcon): ResolvedPluginIcon?
    fun resolveImage(image: PluginImage): Any?
}

val LocalResourceResolver = staticCompositionLocalOf<ResourceResolver> { error("ResourceResolver is not provided") }
