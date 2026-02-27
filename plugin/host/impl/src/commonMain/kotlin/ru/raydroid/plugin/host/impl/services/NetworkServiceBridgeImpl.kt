package ru.raydroid.plugin.host.impl.services

import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge

internal class NetworkServiceBridgeImpl(
    private val httpClient: HttpClient
): NetworkServiceBridge {
    override suspend fun request(request: NetworkServiceBridge.RawNetworkRequest): NetworkServiceBridge.RawNetworkResponse {
        val response = httpClient.request(request.url) {
            method = HttpMethod.parse(request.method)
            headers {
                request.headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
            if (request.body != null) {
                setBody(request.body)
            }
        }
        return NetworkServiceBridge.RawNetworkResponse(
            statusCode = response.status.value,
            body = response.bodyAsBytes()
        )
    }
}