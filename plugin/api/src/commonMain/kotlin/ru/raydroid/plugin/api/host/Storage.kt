package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService

interface Storage: ZiplineService {
    suspend operator fun get(key: String): String?
    suspend operator fun set(key: String, value: String)
    suspend fun has(key: String): Boolean
}