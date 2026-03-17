package ru.raydroid.plugin.host.api

interface PluginRuntimeManager {
    suspend fun load(runtime: SinglePluginRuntime)
    suspend fun unload(runtime: SinglePluginRuntime)
    fun get(): MultiPluginRuntime
}