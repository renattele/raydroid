package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionId
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.PluginArtifact
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.application.usecase.OpenItemUseCase
import ru.raydroid.plugin.host.impl.data.plugin.LocalPluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.PluginRepositoryImpl
import ru.raydroid.plugin.host.impl.data.plugin.RemotePlugin
import ru.raydroid.plugin.host.impl.data.plugin.RemotePluginDataSource
import ru.raydroid.plugin.host.impl.data.plugin.ResourcePluginDataSource
import ru.raydroid.plugin.host.impl.data.search.CachedSearchRanker
import ru.raydroid.plugin.host.impl.data.search.SearchIndexRepositoryImpl
import ru.raydroid.plugin.host.impl.data.search.SearchResourceResolver
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheContentEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheDao
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheMutation
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheSearchEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheWithContent
import ru.raydroid.plugin.host.impl.runtime.PluginRuntimeCoordinatorImpl
import ru.raydroid.plugin.host.api.domain.service.PluginLoader
import ru.raydroid.plugin.host.api.ui.PluginIcon
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListItem
import ru.raydroid.plugin.host.impl.ui.toPluginCommandPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PluginHostArchitectureTest {
    @Test
    fun `search index repository maps cached entities into cached search results`() = runTest {
        val dao = FakeSearchIndexCacheDao(
            recentResults = listOf(
                SearchIndexCacheSearchEntity(
                    cacheId = 1,
                    contentId = 1,
                    pluginId = "ru.test.plugin",
                    command = "apps",
                    itemId = "item-1",
                    icon = null,
                    iconType = null,
                    title = "Calculator",
                    description = "System app",
                    lastUsedAtEpochMs = null,
                    usageCount = 0,
                )
            )
        )
        val repository = SearchIndexRepositoryImpl(
            cacheDao = dao,
            resourceResolver = passthroughResourceResolver(),
            clock = kotlin.time.Clock.System,
            ranker = CachedSearchRanker(),
        )

        val result = repository.search("", limit = 10).first().single()

        val cached = assertIs<SearchResultSet.CachedSearchResult>(result.result)
        assertEquals("ru.test.plugin", cached.resultId.pluginId.id)
        assertEquals("apps", cached.resultId.commandName)
        assertEquals("item-1", cached.resultId.itemId.value)
        assertEquals("Calculator", assertIs<PluginUiText.Plain>(cached.listEntry.title).text)
        assertEquals("System app", assertIs<PluginUiText.Plain>(cached.listEntry.description).text)
    }

    @Test
    fun `search index repository restores builtin icons from cached entities`() = runTest {
        val dao = FakeSearchIndexCacheDao(
            recentResults = listOf(
                SearchIndexCacheSearchEntity(
                    cacheId = 1,
                    contentId = 1,
                    pluginId = "ru.test.plugin",
                    command = "apps",
                    itemId = "item-1",
                    icon = "ArrowDropUp",
                    iconType = Icon.Type.Builtin.name,
                    title = "Calculator",
                    description = "System app",
                    lastUsedAtEpochMs = null,
                    usageCount = 0,
                )
            )
        )
        val repository = SearchIndexRepositoryImpl(
            cacheDao = dao,
            resourceResolver = passthroughResourceResolver(),
            clock = kotlin.time.Clock.System,
            ranker = CachedSearchRanker(),
        )

        val result = repository.search("", limit = 10).first().single()

        val cached = assertIs<SearchResultSet.CachedSearchResult>(result.result)
        assertEquals("ArrowDropUp", assertIs<PluginIcon.Builtin>(cached.listEntry.icon).name)
    }

    @Test
    fun `search index repository persists builtin icons on update`() = runTest {
        val dao = FakeSearchIndexCacheDao(recentResults = emptyList())
        val repository = SearchIndexRepositoryImpl(
            cacheDao = dao,
            resourceResolver = passthroughResourceResolver(),
            clock = kotlin.time.Clock.System,
            ranker = CachedSearchRanker(),
        )

        repository.update(
            listOf(
                SearchIndexMutation.Upsert(
                    resultId = SearchResultId(
                        pluginId = PluginId("ru.test.plugin"),
                        commandName = "apps",
                        itemId = CommandItemId("item-1"),
                    ),
                    listEntry = CommandListItem(
                        id = CommandItemId("item-1"),
                        icon = Icon.Builtin("ArrowDropUp"),
                        title = UiText.Plain("Calculator"),
                        description = UiText.Plain("System app"),
                    )
                )
            )
        )

        val inserted = requireNotNull(dao.lastInserted)
        assertEquals("ArrowDropUp", inserted.searchIndexCache.icon)
        assertEquals(Icon.Type.Builtin.name, inserted.searchIndexCache.iconType)
        assertEquals("calculator", inserted.content.single().titleSearch)
        assertEquals("system app", inserted.content.single().descriptionSearch)
    }

    @Test
    fun `runtime coordinator exposes live content with result ids`() = runTest {
        val listEntry = CommandListItem(
            id = CommandItemId("item-1"),
            icon = null,
            title = null,
            description = null,
        )
        val presentation = CommandPresentation(
            listEntry = listEntry,
            content = emptyList(),
        )
        val runtime = FakePluginRuntime(
            manifest = testManifest(),
            contentItems = MutableStateFlow(
                listOf(
                    PluginRuntime.ContentItem(
                        commandName = "apps",
                        presentation = presentation,
                    )
                )
            )
        )
        val pluginRuntimes = MutableStateFlow(emptyList<PluginRuntime>())
        val coordinator = PluginRuntimeCoordinatorImpl(
            coroutineScope = backgroundScope,
            pluginRuntimes = pluginRuntimes,
        )
        pluginRuntimes.value = listOf(runtime)

        advanceUntilIdle()

        val content = coordinator.content().first { it.isNotEmpty() }.single()
        assertEquals(runtime, content.runtime)
        assertEquals("ru.test.plugin", content.resultId.pluginId.id)
        assertEquals("apps", content.resultId.commandName)
        assertEquals("item-1", content.resultId.itemId.value)
        assertEquals(listEntry.toPluginCommandListItem(runtime.pluginId), content.listEntry)
        assertEquals(presentation.toPluginCommandPresentation(runtime.pluginId), content.presentation)
    }

    @Test
    fun `runtime coordinator exposes searchable command launcher entries`() = runTest {
        val visibleRuntime = FakePluginRuntime(
            manifest = testManifest(name = "ru.test.visible", searchable = true)
        )
        val hiddenRuntime = FakePluginRuntime(
            manifest = testManifest(name = "ru.test.hidden", searchable = false)
        )
        val pluginRuntimes = MutableStateFlow(emptyList<PluginRuntime>())
        val coordinator = PluginRuntimeCoordinatorImpl(
            coroutineScope = backgroundScope,
            pluginRuntimes = pluginRuntimes,
        )
        pluginRuntimes.value = listOf(visibleRuntime, hiddenRuntime)

        advanceUntilIdle()

        val command = coordinator.commands().first { commands -> commands.isNotEmpty() }.single()
        assertEquals(CommandItemId.CommandRoot, command.resultId.itemId)
        assertEquals("ru.test.visible", command.resultId.pluginId.id)
        assertEquals("apps", command.resultId.commandName)
        assertEquals("Apps", assertIs<PluginUiText.Plain>(command.listEntry.title).text)
    }

    @Test
    fun `open item use case dispatches open command action for command root`() = runTest {
        val runtime = FakePluginRuntime(manifest = testManifest())
        val coordinator = FakePluginRuntimeCoordinator(runtimes = MutableStateFlow(listOf(runtime)))
        val searchRepository = RecordingSearchIndexRepository()
        val useCase = OpenItemUseCase(
            pluginRuntimeRegistry = object : PluginRuntimeRegistry {
                override suspend fun load(runtime: PluginRuntime) = Unit
                override suspend fun unload(runtime: PluginRuntime) = Unit
                override fun get(): PluginRuntimeCoordinator = coordinator
            },
            searchIndexRepository = searchRepository,
        )
        val resultId = SearchResultId(
            pluginId = runtime.pluginId,
            commandName = "apps",
            itemId = CommandItemId.CommandRoot,
        )

        useCase(query = "app", resultId = resultId)

        val update = runtime.updates.single()
        assertEquals("app", update.first)
        assertIs<CommandAction.OpenCommand>(update.second)
        assertEquals(resultId, searchRepository.lastUpdatedUsage)
    }

    @Test
    fun `open item use case dispatches enter action and updates usage`() = runTest {
        val runtime = FakePluginRuntime(manifest = testManifest())
        val coordinator = FakePluginRuntimeCoordinator(runtimes = MutableStateFlow(listOf(runtime)))
        val searchRepository = RecordingSearchIndexRepository()
        val useCase = OpenItemUseCase(
            pluginRuntimeRegistry = object : PluginRuntimeRegistry {
                override suspend fun load(runtime: PluginRuntime) = Unit
                override suspend fun unload(runtime: PluginRuntime) = Unit
                override fun get(): PluginRuntimeCoordinator = coordinator
            },
            searchIndexRepository = searchRepository,
        )
        val resultId = SearchResultId(
            pluginId = runtime.pluginId,
            commandName = "apps",
            itemId = CommandItemId("item-1"),
        )

        useCase(query = "calc", resultId = resultId)

        val update = runtime.updates.single()
        assertEquals("calc", update.first)
        assertEquals(CommandAction.Enter(CommandItemId("item-1")), update.second)
        assertEquals(resultId, searchRepository.lastUpdatedUsage)
    }

    @Test
    fun `open item use case dispatches command list action and updates usage`() = runTest {
        val runtime = FakePluginRuntime(manifest = testManifest())
        val coordinator = FakePluginRuntimeCoordinator(runtimes = MutableStateFlow(listOf(runtime)))
        val searchRepository = RecordingSearchIndexRepository()
        val useCase = OpenItemUseCase(
            pluginRuntimeRegistry = object : PluginRuntimeRegistry {
                override suspend fun load(runtime: PluginRuntime) = Unit
                override suspend fun unload(runtime: PluginRuntime) = Unit
                override fun get(): PluginRuntimeCoordinator = coordinator
            },
            searchIndexRepository = searchRepository,
        )
        val resultId = SearchResultId(
            pluginId = runtime.pluginId,
            commandName = "apps",
            itemId = CommandItemId("item-1"),
        )

        useCase(
            query = "calc",
            resultId = resultId,
            actionId = CommandActionId("delete")
        )

        val update = runtime.updates.single()
        assertEquals("calc", update.first)
        assertEquals(
            CommandAction.ExecuteAction(
                itemId = CommandItemId("item-1"),
                actionId = CommandActionId("delete")
            ),
            update.second
        )
        assertEquals(resultId, searchRepository.lastUpdatedUsage)
    }

    @Test
    fun `plugin repository returns plugin artifact from local data source`() = runTest {
        val pluginId = PluginId("ru.test.plugin")
        val localData = byteArrayOf(1, 2, 3)
        val repository = PluginRepositoryImpl(
            remotePluginDataSource = object : RemotePluginDataSource {
                override suspend fun load(url: String): RemotePlugin? = null
            },
            localPluginDataSource = object : LocalPluginDataSource {
                override suspend fun add(pluginId: PluginId, data: ByteArray) = Unit
                override suspend fun load(pluginId: PluginId): ByteArray? = localData
                override suspend fun hash(pluginId: PluginId): String? = null
                override suspend fun signature(pluginId: PluginId): String? = null
                override suspend fun listPlugins(): List<PluginId> = listOf(pluginId)
                override suspend fun delete(pluginId: PluginId) = Unit
            },
            resourcePluginDataSource = object : ResourcePluginDataSource {
                override suspend fun load(pluginId: PluginId): ByteArray? = null
                override suspend fun listPlugins(): List<PluginId> = emptyList()
            },
            pluginLoader = object : PluginLoader {
                override suspend fun loadPlugin(plugin: PluginArtifact): PluginRuntime? = null
                override suspend fun loadPluginMetadata(plugin: PluginArtifact) = error("unused")
                override suspend fun join(plugins: StateFlow<List<PluginRuntime>>) = error("unused")
            },
        )

        val artifact = repository.loadPlugin(pluginId)

        assertEquals(PluginArtifact(localData), artifact)
    }
}

private fun passthroughResourceResolver(): SearchResourceResolver {
    return object : SearchResourceResolver {
        override fun session(): SearchResourceResolver.Session {
            return object : SearchResourceResolver.Session {
                override suspend fun resolveContent(
                    pluginId: PluginId,
                    title: ru.raydroid.plugin.api.model.UiText?,
                    description: ru.raydroid.plugin.api.model.UiText?
                ): List<SearchResourceResolver.ResolvedContent> {
                    return listOf(
                        SearchResourceResolver.ResolvedContent(
                            title = title?.text,
                            description = description?.text,
                        )
                    )
                }

                override suspend fun resolveIcon(
                    pluginId: PluginId,
                    icon: ru.raydroid.plugin.api.ui.Icon?
                ) = icon
            }
        }
    }
}

private fun testManifest(name: String = "ru.test.plugin", searchable: Boolean = true): Manifest {
    return Manifest(
        name = name,
        title = UiText.Plain("Test"),
        description = UiText.Plain("Test manifest"),
        author = UiText.Plain("Codex"),
        version = 1,
        platforms = listOf(Platform.MacOS),
        categories = emptyList(),
        license = "MIT",
        commands = listOf(
            Command(
                service = "apps",
                title = UiText.Plain("Apps"),
                description = UiText.Plain("Apps command"),
                mode = Command.Mode.View,
                match = null,
                searchable = searchable,
                arguments = emptyList(),
                preferences = emptyList(),
            )
        ),
        resources = emptyMap(),
    )
}

private class FakePluginRuntime(
    override val manifest: Manifest,
    private val contentItems: MutableStateFlow<List<PluginRuntime.ContentItem>> = MutableStateFlow(emptyList()),
) : PluginRuntime {
    override val pluginId: PluginId = PluginId(manifest.name)
    override val resources: FileSystem = FakeFileSystem()
    val updates = mutableListOf<Pair<String, CommandAction>>()
    private val fullscreen = MutableStateFlow<PluginRuntime.FullscreenContent?>(null)

    override fun cachedItems(chunkSize: Int): Flow<List<SearchIndexMutation>> = emptyFlow()

    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> = contentItems

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> = fullscreen

    override suspend fun update(query: String, action: CommandAction) {
        updates += query to action
    }

    override suspend fun update(commandName: String, query: String, action: CommandAction) {
        update(query, action)
    }

    override suspend fun unload() = Unit
}

private class FakePluginRuntimeCoordinator(
    private val runtimes: StateFlow<List<PluginRuntime>>,
) : PluginRuntimeCoordinator {
    override fun cachedItems(): Flow<Map<PluginRuntime, List<SearchIndexMutation>>> = emptyFlow()

    override fun runtimes(): StateFlow<List<PluginRuntime>> = runtimes

    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> =
        MutableStateFlow(emptyList())

    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> =
        MutableStateFlow(emptyList())

    override suspend fun update(query: String, action: CommandAction) = Unit
}

private class RecordingSearchIndexRepository : SearchIndexRepository {
    var lastUpdatedUsage: SearchResultId? = null

    override suspend fun update(mutations: List<SearchIndexMutation>) = Unit

    override suspend fun updateUsage(resultId: SearchResultId) {
        lastUpdatedUsage = resultId
    }

    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> =
        flowOf(emptyList())
}

private class FakeSearchIndexCacheDao(
    private val recentResults: List<SearchIndexCacheSearchEntity>,
) : SearchIndexCacheDao() {
    var lastInserted: SearchIndexCacheWithContent? = null

    override suspend fun insert(entity: SearchIndexCacheWithContent) {
        lastInserted = entity
    }

    override suspend fun insertListItem(entity: SearchIndexCacheEntity): Long = 0
    override suspend fun deleteListItem(pluginId: String, command: String, itemId: String) = Unit
    override suspend fun updateListItem(entity: SearchIndexCacheEntity) = Unit
    override suspend fun insertContent(entities: List<SearchIndexCacheContentEntity>) = Unit
    override suspend fun getSearchIndexId(pluginId: String, command: String, itemId: String): Long? = null
    override suspend fun deleteContentBySearchIndexCacheId(searchIndexCacheId: Long) = Unit
    override suspend fun deleteContentByCommand(pluginId: String, command: String) = Unit
    override suspend fun deleteCommandListItems(pluginId: String, command: String) = Unit
    override suspend fun deleteOutdatedContentByCommand(pluginId: String, commandName: String) = Unit
    override suspend fun deleteOutdatedCommandListItems(pluginId: String, commandName: String) = Unit
    override suspend fun updateUsage(pluginId: String, command: String, itemId: String, nowEpochMs: Long) = Unit
    override suspend fun markAsOutdated(pluginId: String, commandName: String, itemId: String) = Unit
    override suspend fun markAllAsOutdated(pluginId: String, commandName: String) = Unit
    override fun recent(limit: Int): Flow<List<SearchIndexCacheSearchEntity>> = flowOf(recentResults.take(limit))
    override fun searchFtsCandidates(matchQuery: String, limit: Int): Flow<List<SearchIndexCacheSearchEntity>> =
        flowOf(emptyList())
    override fun searchFallbackCandidates(limit: Int): Flow<List<SearchIndexCacheSearchEntity>> =
        flowOf(emptyList())
}
