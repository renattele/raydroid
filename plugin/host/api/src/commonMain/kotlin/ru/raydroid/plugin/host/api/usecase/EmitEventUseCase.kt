package ru.raydroid.plugin.host.api.usecase

import ru.raydroid.plugin.host.api.EventGateway
import ru.raydroid.plugin.host.api.PluginId

class EmitEventUseCase(
    private val eventGateway: EventGateway
) {
    suspend operator fun invoke(pluginId: PluginId, data: Any) {
        eventGateway.emit(pluginId, data)
    }
}