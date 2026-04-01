package ru.raydroid.plugin.host.impl.data.plugin

interface RemotePluginDataSource {
    suspend fun load(url: String): RemotePlugin?
}