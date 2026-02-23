package ru.raydroid.plugin.api.host.impl

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.bridge.StorageServiceBridge
import ru.raydroid.plugin.api.host.service.StorageService

internal class StorageServiceImpl(
    private val bridge: StorageServiceBridge,
    private val serializer: Json
): StorageService {
    override suspend fun <T : Any> get(key: String, strategy: DeserializationStrategy<T>): T? {
        return bridge[key]?.let {
            serializer.decodeFromString(strategy, it)
        }
    }

    override suspend fun <T : Any> set(key: String, strategy: SerializationStrategy<T>, value: T) {
        bridge[key] = serializer.encodeToString(strategy, value)
    }

}