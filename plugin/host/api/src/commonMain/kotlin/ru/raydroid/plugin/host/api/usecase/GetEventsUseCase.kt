package ru.raydroid.plugin.host.api.usecase

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.EventGateway
import ru.raydroid.plugin.host.api.PluginEvent

class GetEventsUseCase(
    private val eventGateway: EventGateway
) {
    operator fun invoke(): Flow<PluginEvent<*>> {
        return eventGateway.get()
    }
}