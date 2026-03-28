package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.ui.Icon

interface SystemService {
    suspend fun getApps(): List<Application>
    suspend fun openApp(appId: String, options: OpenOptions = OpenOptions())
    suspend fun open(target: String, options: OpenOptions = OpenOptions())

    @Serializable
    data class OpenOptions(
        val keys: Map<String, String> = emptyMap()
    )

    @Serializable
    data class Application(
        val name: String?,
        val id: String,
        val icon: Icon
    )
}

