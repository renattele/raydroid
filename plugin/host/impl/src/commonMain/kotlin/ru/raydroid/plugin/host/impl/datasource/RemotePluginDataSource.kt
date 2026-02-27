package ru.raydroid.plugin.host.impl.datasource

interface RemotePluginDataSource {
    suspend fun load(url: String): RemotePlugin?
}