package ru.raydroid.plugin.api.core

import app.cash.zipline.Zipline
import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    Android,
    IOS
}

@Serializable
data class Manifest(
    val name: String,
    val title: UiText,
    val description: UiText,
    val author: UiText,
    val version: Int,
    val platforms: List<Platform>,
    val categories: List<String>,
    val license: String,
    val commands: List<Command>,
)

interface ManifestService: ZiplineService {
    fun getManifest(): Manifest
}