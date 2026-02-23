package ru.raydroid.plugin.api.host.service

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

interface PreferencesService {
    operator fun <T: Any> get(key: String, strategy: DeserializationStrategy<T>): Flow<T?>
    suspend operator fun <T: Any> set(key: String, strategy: SerializationStrategy<T>, value: T)
}