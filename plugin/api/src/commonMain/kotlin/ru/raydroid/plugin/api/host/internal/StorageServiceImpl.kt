package ru.raydroid.plugin.api.host.internal

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.service.StorageService
import ru.raydroid.plugin.api.host.transport.StorageServiceBridge

internal class StorageServiceImpl(
    private val bridge: StorageServiceBridge,
    private val serializer: Json,
) : StorageService {
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
}
