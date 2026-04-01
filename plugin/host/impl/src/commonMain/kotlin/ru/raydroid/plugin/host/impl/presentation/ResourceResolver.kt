package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.runtime.staticCompositionLocalOf
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginImage
import ru.raydroid.plugin.host.api.ui.PluginUiText

interface ResourceResolver {
    fun resolveText(text: PluginUiText): String
    fun resolveIcon(icon: PluginIcon): Any?
    fun resolveImage(image: PluginImage): Any?
}

val LocalResourceResolver = staticCompositionLocalOf<ResourceResolver> { error("ResourceResolver is not provided") }
