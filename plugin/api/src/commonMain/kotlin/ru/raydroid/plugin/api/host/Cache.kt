package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow

interface Cache: ZiplineService {
    suspend operator fun get(key: String): String?
    suspend operator fun set(key: String, value: String)
    suspend fun has(key: String): Boolean

    suspend fun clear()
    suspend fun <T: Any> flowOf(key: String): Flow<T>
}