package ru.raydroid.plugin.host.api

import kotlinx.coroutines.flow.StateFlow

interface PluginLoader {
    suspend fun loadPlugin(
        plugin: Plugin
    ): SinglePluginRuntime?

    suspend fun loadPluginMetadata(
        plugin: Plugin
    ): PluginMetadata?

    suspend fun join(
        plugins: StateFlow<List<SinglePluginRuntime>>
    ): MultiPluginRuntime
}