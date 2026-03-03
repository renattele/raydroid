package ru.raydroid.plugin.host.api.usecase

import kotlinx.coroutines.flow.collectLatest
import ru.raydroid.plugin.host.api.PluginRuntimeManager
import ru.raydroid.plugin.host.api.SearchRepository

class SyncCacheUseCase(
    private val searchRepository: SearchRepository,
    private val runtimeManager: PluginRuntimeManager
) {
    suspend operator fun invoke() {
        val multiRuntime = runtimeManager.get()
        multiRuntime.cachedItems().collectLatest { cachedItems ->
            cachedItems.forEach { (_, items) ->
                searchRepository.update(items)
            }
        }
    }
}