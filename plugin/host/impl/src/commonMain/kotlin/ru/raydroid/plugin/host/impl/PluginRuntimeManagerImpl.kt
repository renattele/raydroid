package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.raydroid.plugin.host.api.MultiPluginRuntime
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SinglePluginRuntime

class PluginRuntimeManagerImpl(private val coroutineScope: CoroutineScope): PluginRuntimeManager {
    private val runtimeListFlow = MutableStateFlow<List<SinglePluginRuntime>>(emptyList())
    private val runtime by lazy {
        MultiPluginRuntimeImpl(coroutineScope, runtimeListFlow)
    }
    override suspend fun load(runtime: SinglePluginRuntime) {
       runtimeListFlow.update { runtimeList ->
           runtimeList + runtime
       }
    }

    override suspend fun unload(runtime: SinglePluginRuntime) {
        runtimeListFlow.update { runtimeList ->
            runtimeList - runtime
        }
    }

    override fun get(): MultiPluginRuntime = runtime
}