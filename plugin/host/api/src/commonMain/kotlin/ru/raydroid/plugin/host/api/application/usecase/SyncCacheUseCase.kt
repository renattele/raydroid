package ru.raydroid.plugin.host.api.application.usecase

import kotlinx.coroutines.flow.collectLatest
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository

class SyncCacheUseCase(
    private val searchIndexRepository: SearchIndexRepository,
    private val pluginRuntimeRegistry: PluginRuntimeRegistry
) {
    suspend operator fun invoke() {
        val runtimeCoordinator = pluginRuntimeRegistry.get()
        runtimeCoordinator.cachedItems().collectLatest { cachedItems ->
            cachedItems.forEach { (_, mutations) ->
                searchIndexRepository.update(mutations)
            }
        }
    }
}
