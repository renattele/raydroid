package ru.raydroid.plugin.host.api

interface ResourceRepository {
    suspend fun getString(pluginId: PluginId, resourceName: String): String?
    suspend fun getBinary(pluginId: PluginId, resourceName: String): String?
}