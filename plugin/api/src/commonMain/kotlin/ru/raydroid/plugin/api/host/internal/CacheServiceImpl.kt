package ru.raydroid.plugin.api.host.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.service.CacheService
import ru.raydroid.plugin.api.host.transport.CacheServiceBridge

internal class CacheServiceImpl(
    private val bridge: CacheServiceBridge,
    private val serializer: Json,
) : CacheService {
    override suspend fun <T : Any> get(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): T? =
        bridge[key]?.let {
            serializer.decodeFromString(strategy, it)
        }

    override suspend fun <T : Any> set(
        key: String,
        strategy: SerializationStrategy<T>,
        value: T,
    ) {
        bridge[key] = serializer.encodeToString(strategy, value)
    }

    override suspend fun clear() {
        bridge.clear()
    }

    override suspend fun <T : Any> flowOf(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): Flow<T?> =
        bridge.flowOf(key).map { value ->
            value?.let { serializer.decodeFromString(strategy, it) }
        }
}
