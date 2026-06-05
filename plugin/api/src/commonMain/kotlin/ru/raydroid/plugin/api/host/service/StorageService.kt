package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

interface StorageService {
    suspend operator fun <T : Any> get(
        key: String,
        strategy: DeserializationStrategy<T>,
    ): T?

    suspend operator fun <T : Any> set(
        key: String,
        strategy: SerializationStrategy<T>,
        value: T,
    )
}
