package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface System: ZiplineService {
    suspend fun getApps(): List<Application>
    suspend fun open(app: Application, options: OpenOptions = OpenOptions())
    suspend fun open(target: String, options: OpenOptions = OpenOptions())

    @Serializable
    data class OpenOptions(
        val keys: Map<String, String> = emptyMap()
    )
}

@Serializable
data class Application(
    val name: String,
    val id: String,
)