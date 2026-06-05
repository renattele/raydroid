package ru.raydroid.plugin.host.api.domain.runtime

interface PluginRuntimeRegistry {
    suspend fun load(runtime: PluginRuntime)

    suspend fun unload(runtime: PluginRuntime)

    fun get(): PluginRuntimeCoordinator
}
