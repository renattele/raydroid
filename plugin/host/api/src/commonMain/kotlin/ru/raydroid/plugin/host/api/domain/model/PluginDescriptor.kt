package ru.raydroid.plugin.host.api.domain.model

import okio.FileSystem
import ru.raydroid.plugin.api.manifest.Manifest

interface PluginDescriptor {
    val pluginId: PluginId
    val manifest: Manifest
    val resources: FileSystem
}
