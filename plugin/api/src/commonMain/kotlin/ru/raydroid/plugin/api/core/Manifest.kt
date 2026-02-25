package ru.raydroid.plugin.api.core

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
    val title: String,
    val description: String,
    val author: String,
    val version: Int,
    val platforms: List<Platform>,
    val categories: List<String>,
    val license: String,
    val commands: List<Command>,
    val resources: Resources
)

typealias Resources = Map<String, Map<String, String>>

interface ManifestService: ZiplineService {
    fun getManifest(): Manifest
}