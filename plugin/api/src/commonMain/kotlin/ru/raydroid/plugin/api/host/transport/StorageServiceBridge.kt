package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService

interface StorageServiceBridge : ZiplineService {
    suspend operator fun get(key: String): String?

    suspend operator fun set(
        key: String,
        value: String,
    )

    suspend fun has(key: String): Boolean
}
