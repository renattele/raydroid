package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import ru.raydroid.plugin.api.manifest.Platform

interface EnvironmentServiceBridge : ZiplineService {
    fun get(key: String): String?

    val platform: Platform
}
