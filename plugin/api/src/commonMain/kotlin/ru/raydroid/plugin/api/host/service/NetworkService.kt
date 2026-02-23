package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy

interface NetworkService {
    suspend fun request(
        url: String,
        body: ByteArray? = null,
        requestType: RequestType = RequestType.GET,
        headers: Map<String, String> = emptyMap(),
    ): NetworkResponse

    suspend fun <T: Any, R: Any> request(
        url: String,
        body: T? = null,
        requestType: RequestType = RequestType.GET,
        headers: Map<String, String> = emptyMap(),
        requestStrategy: SerializationStrategy<T>,
        responseStrategy: DeserializationStrategy<R>
    ): TypedNetworkResponse<R>

    enum class RequestType {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD
    }
    @Serializable
    data class TypedNetworkResponse<T: Any>(
        val statusCode: Int,
        val body: T?
    )

    @Serializable
    data class NetworkResponse(
        val statusCode: Int,
        val body: ByteArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as NetworkResponse

            if (statusCode != other.statusCode) return false
            if (!body.contentEquals(other.body)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = statusCode
            result = 31 * result + (body?.contentHashCode() ?: 0)
            return result
        }
    }

}

