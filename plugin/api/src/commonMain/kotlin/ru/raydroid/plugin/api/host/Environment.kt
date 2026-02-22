package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService

interface Environment: ZiplineService {
    fun get(key: String): String?
}