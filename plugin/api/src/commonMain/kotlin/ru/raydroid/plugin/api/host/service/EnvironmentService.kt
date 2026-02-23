package ru.raydroid.plugin.api.host.service

import ru.raydroid.plugin.api.core.Platform

interface EnvironmentService {
    fun get(key: String): String?

    val platform: Platform
}