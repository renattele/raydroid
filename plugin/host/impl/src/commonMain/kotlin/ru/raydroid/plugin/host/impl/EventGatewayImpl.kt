package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import ru.raydroid.plugin.host.api.EventGateway
import ru.raydroid.plugin.host.api.PluginEvent
import ru.raydroid.plugin.host.api.PluginId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EventGatewayImpl : EventGateway {
    private val events = MutableSharedFlow<PluginEvent<*>>()
    override fun get(): Flow<PluginEvent<*>> = events

    override fun get(pluginId: PluginId): Flow<PluginEvent<*>> = get().filter {
        it.pluginId == pluginId
    }

    override suspend fun emit(pluginId: PluginId, data: Any) {
        events.emit(PluginEvent(generateId(), pluginId, data))
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateId(): PluginEvent.Id {
        return PluginEvent.Id(Uuid.generateV4().toString())
    }
}