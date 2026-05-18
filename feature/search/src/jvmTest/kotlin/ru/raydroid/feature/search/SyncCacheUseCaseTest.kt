package ru.raydroid.feature.search

import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.SyncCacheUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCacheUseCaseTest {
    @Test
    fun `applies fast cache chunks in order without dropping in-flight updates`() = runTest {
        val pluginId = PluginId("ru.raydroid.files")
        val first = SearchIndexMutation.MarkAllAsOutdated(pluginId, "files")
        val second = SearchIndexMutation.ClearOutdated(pluginId, "files")
        val cachedItems = MutableSharedFlow<List<SearchIndexMutation>>(extraBufferCapacity = 2)
        val searchRepository = SlowRecordingSearchIndexRepository()
        val useCase = SyncCacheUseCase(
            searchIndexRepository = searchRepository,
            pluginRuntimeRegistry = SyncCacheRegistry(SyncCacheCoordinator(cachedItems))
        )

        val job = launch { useCase() }
        runCurrent()
        cachedItems.emit(listOf(first))
        cachedItems.emit(listOf(second))
        advanceUntilIdle()

        assertEquals(listOf(listOf(first), listOf(second)), searchRepository.updates)
        job.cancel()
    }
}

private class SyncCacheCoordinator(
    private val cachedItems: Flow<List<SearchIndexMutation>>
) : PluginRuntimeCoordinator {
    override fun cachedItems(): Flow<List<SearchIndexMutation>> = cachedItems
    override fun runtimes(): StateFlow<List<PluginRuntime>> = MutableStateFlow(emptyList())
    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> = MutableStateFlow(emptyList())
    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> = MutableStateFlow(emptyList())
    override suspend fun update(action: CommandActionBridge) = Unit
}

private class SyncCacheRegistry(
    private val coordinator: PluginRuntimeCoordinator
) : PluginRuntimeRegistry {
    override suspend fun load(runtime: PluginRuntime) = Unit
    override suspend fun unload(runtime: PluginRuntime) = Unit
    override fun get(): PluginRuntimeCoordinator = coordinator
}

private class SlowRecordingSearchIndexRepository : SearchIndexRepository {
    val updates = mutableListOf<List<SearchIndexMutation>>()

    override suspend fun update(mutations: List<SearchIndexMutation>) {
        delay(100)
        updates += mutations
    }

    override suspend fun updateUsage(resultId: SearchResultId) = Unit
    override suspend fun getPreview(resultId: SearchResultId): RankedSearchResult? = null
    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> = emptyFlow()
}
