package ru.raydroid.plugin.host.api

import okio.FileSystem
import ru.raydroid.plugin.api.core.Manifest

interface PluginMetadata {
    val pluginId: PluginId
    val manifest: Manifest
    val resource: FileSystem
}