package ru.raydroid.plugin.api.host.service

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlin.reflect.KClass

interface CacheService {
    suspend operator fun <T : Any> get(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): T?

    suspend operator fun <T : Any> set(
        key: String,
        strategy: SerializationStrategy<T>,
        value: T,
    )

    suspend fun clear()

    suspend fun <T : Any> flowOf(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): Flow<T?>
}
