package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import ru.raydroid.plugin.api.host.service.SearchFieldState

interface SearchFieldServiceBridge : ZiplineService {
    suspend fun setState(state: SearchFieldState)
}
