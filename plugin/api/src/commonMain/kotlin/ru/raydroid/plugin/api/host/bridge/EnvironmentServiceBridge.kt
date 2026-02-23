package ru.raydroid.plugin.api.host.bridge

import app.cash.zipline.ZiplineService
import ru.raydroid.plugin.api.core.Platform

interface EnvironmentServiceBridge: ZiplineService {
    fun get(key: String): String?

    val platform: Platform
}