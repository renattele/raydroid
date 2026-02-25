package ru.raydroid.plugin.host

import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.Manifest

data class Plugin(
    val manifest: Manifest,
    val commandServices: List<CommandServiceBridge>,
    val resources: FileSystem,
    val signature: String? = null
)