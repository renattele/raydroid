package ru.raydroid.plugin.host.impl.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.event.EventGateway
import ru.raydroid.plugin.host.api.event.PluginEvent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EventGatewayImpl : EventGateway {
    private val events = MutableSharedFlow<PluginEvent<*>>()

    override fun get(): Flow<PluginEvent<*>> = events

    override fun get(pluginId: PluginId): Flow<PluginEvent<*>> =
        get().filter {
            it.pluginId == pluginId
        }

    override suspend fun emit(
        pluginId: PluginId,
        data: Any,
    ) {
        events.emit(PluginEvent(generateId(), pluginId, data))
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateId(): PluginEvent.Id = PluginEvent.Id(Uuid.generateV4().toString())
}
