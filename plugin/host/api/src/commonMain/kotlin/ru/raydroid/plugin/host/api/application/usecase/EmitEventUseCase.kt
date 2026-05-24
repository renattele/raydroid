package ru.raydroid.plugin.host.api.application.usecase

import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.EventGateway

class EmitEventUseCase(
    private val eventGateway: EventGateway,
) {
    suspend operator fun invoke(
        pluginId: PluginId,
        data: Any,
    ) {
        eventGateway.emit(pluginId, data)
    }
}
