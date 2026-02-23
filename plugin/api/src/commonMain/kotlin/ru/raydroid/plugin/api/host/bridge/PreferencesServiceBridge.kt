package ru.raydroid.plugin.api.host.bridge

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow

interface PreferencesServiceBridge: ZiplineService {
    operator fun get(key: String): Flow<String?>
    suspend operator fun set(key: String, value: String)
}