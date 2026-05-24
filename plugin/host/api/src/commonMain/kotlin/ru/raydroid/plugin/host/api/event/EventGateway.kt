package ru.raydroid.plugin.host.api.event

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.host.api.domain.model.PluginId
import kotlin.jvm.JvmInline

interface EventGateway {
    fun get(): Flow<PluginEvent<*>>

    fun get(pluginId: PluginId): Flow<PluginEvent<*>>

    suspend fun emit(
        pluginId: PluginId,
        data: Any,
    )
}

data class PluginEvent<out T>(
    val id: Id,
    val pluginId: PluginId,
    val data: T,
) {
    @JvmInline
    value class Id(
        val value: String,
    )
}
