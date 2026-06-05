package ru.raydroid.plugin.host.impl.services

import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.host.transport.SearchFieldServiceBridge
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.SearchFieldGateway
import ru.raydroid.plugin.host.api.event.SearchFieldRequest

internal class SearchFieldServiceBridgeImpl(
    private val pluginId: PluginId,
    private val searchFieldGateway: SearchFieldGateway,
) : SearchFieldServiceBridge {
    override suspend fun setState(state: SearchFieldState) {
        searchFieldGateway.emit(
            SearchFieldRequest(
                pluginId = pluginId,
                state = state,
            ),
        )
    }
}
