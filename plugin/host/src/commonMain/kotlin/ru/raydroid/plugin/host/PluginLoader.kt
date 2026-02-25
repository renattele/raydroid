package ru.raydroid.plugin.host

import kotlinx.coroutines.flow.Flow
import okio.FileSystem
import ru.raydroid.plugin.api.core.CommandServiceBridge
import ru.raydroid.plugin.api.core.Manifest

interface PluginLoader {
    suspend fun loadPlugin(
        pluginBytes: ByteArray
    ): PluginLoadResult
}


sealed class PluginLoadResult {
    data class Success(
        val plugin: Plugin
    ): PluginLoadResult()


    data class Failure(val exception: Exception): PluginLoadResult()
}