package ru.raydroid.plugin.api.host.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import ru.raydroid.plugin.api.host.service.PreferencesService
import ru.raydroid.plugin.api.host.transport.PreferencesServiceBridge

internal class PreferencesServiceImpl(
    private val bridge: PreferencesServiceBridge,
    private val serializer: Json,
) : PreferencesService {
    override fun <T : Any> get(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): Flow<T?> =
        bridge[key].map { value ->
            value?.let {
                serializer.decodeFromString(strategy, it)
            }
        }

    override suspend fun <T : Any> set(
        key: String,
        strategy: SerializationStrategy<T>,
        value: T,
    ) {
        bridge[key] = serializer.encodeToString(strategy, value)
    }
}
