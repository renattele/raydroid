package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface NetworkServiceBridge : ZiplineService {
    suspend fun request(
        request: RawNetworkRequest
    ): RawNetworkResponse

    @Serializable
    data class RawNetworkRequest(
        val url: String,
        val method: String,
        val headers: Map<String, String>,
        val body: ByteArray?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as RawNetworkRequest

            if (url != other.url) return false
            if (method != other.method) return false
            if (headers != other.headers) return false
            if (!body.contentEquals(other.body)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + method.hashCode()
            result = 31 * result + headers.hashCode()
            result = 31 * result + (body?.contentHashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class RawNetworkResponse(
        val statusCode: Int,
        val body: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as RawNetworkResponse

            if (statusCode != other.statusCode) return false
            if (!body.contentEquals(other.body)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = statusCode
            result = 31 * result + body.contentHashCode()
            return result
        }
    }
}

