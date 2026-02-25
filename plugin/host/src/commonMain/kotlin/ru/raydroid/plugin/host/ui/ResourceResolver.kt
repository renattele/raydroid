package ru.raydroid.plugin.host.ui

import androidx.compose.runtime.staticCompositionLocalOf

interface ResourceResolver {
    fun resolveString(resource: String): String
    fun resolveImage(resource: String): ByteArray?
}

val LocalResourceResolver = staticCompositionLocalOf<ResourceResolver> { error("ResourceResolver is not provided") }