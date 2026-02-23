package ru.raydroid.plugin.api.host.impl

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.bridge.NetworkServiceBridge
import ru.raydroid.plugin.api.host.service.NetworkService

internal class NetworkServiceImpl(
    private val bridge: NetworkServiceBridge,
    private val serializer: Json
): NetworkService {
    override suspend fun request(
        url: String,
        body: ByteArray?,
        requestType: NetworkService.RequestType,
        headers: Map<String, String>
    ): NetworkService.NetworkResponse {
        val response = bridge.request(
            NetworkServiceBridge.RawNetworkRequest(
                url = url,
                body = body,
                method = requestType.toString(),
                headers = headers
            )
        )
        return NetworkService.NetworkResponse(
            statusCode = response.statusCode,
            body = body
        )
    }

    override suspend fun <T : Any, R : Any> request(
        url: String,
        body: T?,
        requestType: NetworkService.RequestType,
        headers: Map<String, String>,
        requestStrategy: SerializationStrategy<T>,
        responseStrategy: DeserializationStrategy<R>
    ): NetworkService.TypedNetworkResponse<R> {
        val response = request(
            url = url,
            body = body?.let { serializer.encodeToString(requestStrategy, it).encodeToByteArray() },
            requestType = requestType,
            headers = headers
        )
        return NetworkService.TypedNetworkResponse(
            statusCode = response.statusCode,
            body = response.body?.let { body ->
                serializer.decodeFromString(responseStrategy, body.decodeToString())
            }
        )
    }
}