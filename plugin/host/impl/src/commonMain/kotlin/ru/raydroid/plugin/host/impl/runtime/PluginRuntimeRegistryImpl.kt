package ru.raydroid.plugin.host.impl.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry

class PluginRuntimeRegistryImpl(
    private val coroutineScope: CoroutineScope,
) : PluginRuntimeRegistry {
    private val runtimeListFlow = MutableStateFlow<List<PluginRuntime>>(emptyList())
    private val runtime by lazy {
        PluginRuntimeCoordinatorImpl(coroutineScope, runtimeListFlow)
    }

    override suspend fun load(runtime: PluginRuntime) {
        runtimeListFlow.update { runtimeList ->
            runtimeList + runtime
        }
    }

    override suspend fun unload(runtime: PluginRuntime) {
        runtimeListFlow.update { runtimeList ->
            runtimeList - runtime
        }
    }

    override fun get(): PluginRuntimeCoordinator = runtime
}
