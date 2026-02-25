package ru.raydroid.plugin.host

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.Manifest

interface PluginLoader {
    suspend fun loadPlugin(
        pluginBytes: ByteArray
    ): PluginLoadResult
}


sealed class PluginLoadResult {
    data class Success(
        val manifest: Manifest,
        val commandServices: List<CommandServiceBridge>
    ): PluginLoadResult()


    data class Failure(val exception: Exception): PluginLoadResult()
}