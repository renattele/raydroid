package ru.raydroid.plugin.host.impl.datasource

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RemotePluginDataSourceImpl(
    private val httpClient: HttpClient
): RemotePluginDataSource {
    override suspend fun load(url: String): RemotePlugin? = withContext(Dispatchers.IO) {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            return@withContext null
        }
        val body = response.bodyAsBytes()
        RemotePlugin(body)
    }
}