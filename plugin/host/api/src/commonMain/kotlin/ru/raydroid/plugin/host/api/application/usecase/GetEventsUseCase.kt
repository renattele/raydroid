package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.event.EventGateway
import ru.raydroid.plugin.host.api.event.PluginEvent

class GetEventsUseCase(
    private val eventGateway: EventGateway
) {
    operator fun invoke(): Flow<PluginEvent<*>> {
        return eventGateway.get()
    }
}