package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow

interface CacheServiceBridge : ZiplineService {
    suspend operator fun get(key: String): String?

    suspend operator fun set(
        key: String,
        value: String,
    )

    suspend fun clear()

    suspend fun flowOf(key: String): Flow<String?>
}
