package ru.raydroid.plugin.api.host.bridge

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface SystemServiceBridge: ZiplineService {
    suspend fun getApps(): List<RawApplication>
    suspend fun open(app: RawApplication, options: OpenOptions = OpenOptions())
    suspend fun open(target: String, options: OpenOptions = OpenOptions())

    @Serializable
    data class OpenOptions(
        val keys: Map<String, String> = emptyMap()
    )

    @Serializable
    data class RawApplication(
        val name: String,
        val id: String,
    )
}

