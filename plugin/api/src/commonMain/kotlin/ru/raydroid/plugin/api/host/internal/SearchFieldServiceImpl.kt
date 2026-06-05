package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.host.service.SearchFieldService
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.host.transport.SearchFieldServiceBridge

internal class SearchFieldServiceImpl(
    private val bridge: SearchFieldServiceBridge,
) : SearchFieldService {
    override suspend fun setState(state: SearchFieldState) {
        bridge.setState(state)
    }
}
