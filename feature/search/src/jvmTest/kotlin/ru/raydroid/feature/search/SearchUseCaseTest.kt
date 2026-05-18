package ru.raydroid.feature.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.runtime.CommandActionBridge
import ru.raydroid.plugin.host.api.application.usecase.SearchUseCase
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchAliasEntry
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultScore
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository
import ru.raydroid.plugin.host.api.domain.repository.SearchIndexRepository
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeRegistry
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginCommandPresentation
import ru.raydroid.plugin.host.api.ui.PluginUiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchUseCaseTest {
    @Test
    fun `exact alias promotes command even when ranker returns no command results`() = runTest {
        val fixture = SearchUseCaseFixture(includeCommands = false, includeLive = false)
        fixture.commands.value = listOf(
            PluginRuntimeCoordinator.CommandItem(
                runtime = fixture.runtime,
                listEntry = fixture.commandEntry(),
                resultId = fixture.resultId
            )
        )
        fixture.aliasRepository.aliases.value = mapOf(fixture.resultId to "oy")

        val results = fixture.useCase("oy").first().results

        assertEquals(1, results.size)
        val commandResult = assertIs<SearchResultSet.CommandSearchResult>(results.first())
        assertEquals("oy", commandResult.listEntry.alias)
    }

    @Test
    fun `exact alias promotes cached preview when search index returns no matches`() = runTest {
        val fixture = SearchUseCaseFixture(includeCommands = false, includeLive = false)
        fixture.aliasRepository.aliases.value = mapOf(fixture.resultId to "oy")
        fixture.searchIndexRepository.preview = RankedSearchResult(
            result = SearchResultSet.CachedSearchResult(
                resultId = fixture.resultId,
                listEntry = fixture.commandEntry(),
                titleMatches = emptyList(),
                descriptionMatches = emptyList()
            ),
            score = SearchResultScore(textScore = 0.0)
        )

        val results = fixture.useCase("oy").first().results

        assertEquals(1, results.size)
        val cachedResult = assertIs<SearchResultSet.CachedSearchResult>(results.first())
        assertEquals("oy", cachedResult.listEntry.alias)
    }

    @Test
    fun `alias does not match by prefix`() = runTest {
        val fixture = SearchUseCaseFixture(includeCommands = false, includeLive = false)
        fixture.commands.value = listOf(
            PluginRuntimeCoordinator.CommandItem(
                runtime = fixture.runtime,
                listEntry = fixture.commandEntry(),
                resultId = fixture.resultId
            )
        )
        fixture.aliasRepository.aliases.value = mapOf(fixture.resultId to "oy")

        val results = fixture.useCase("o").first().results

        assertEquals(emptyList(), results)
    }

    @Test
    fun `exact alias keeps merged live result ahead of cached preview for same id`() = runTest {
        val fixture = SearchUseCaseFixture(includeCommands = false, includeLive = true)
        fixture.content.value = listOf(
            PluginRuntimeCoordinator.ContentItem(
                runtime = fixture.runtime,
                presentation = PluginCommandPresentation(
                    listEntry = fixture.commandEntry(),
                    primaryCallback = null,
                    content = emptyList()
                ),
                listEntry = fixture.commandEntry(),
                resultId = fixture.resultId
            )
        )
        fixture.aliasRepository.aliases.value = mapOf(fixture.resultId to "oy")
        fixture.searchIndexRepository.preview = RankedSearchResult(
            result = SearchResultSet.CachedSearchResult(
                resultId = fixture.resultId,
                listEntry = fixture.commandEntry(),
                titleMatches = emptyList(),
                descriptionMatches = emptyList()
            ),
            score = SearchResultScore(textScore = 0.0)
        )

        val result = fixture.useCase("oy").first().results.first()

        assertIs<SearchResultSet.LiveSearchResult>(result)
    }

    @Test
    fun `decorate aliases clears stale alias when repository has no alias`() = runTest {
        val fixture = SearchUseCaseFixture(includeCommands = true, includeLive = false)
        fixture.commands.value = listOf(
            PluginRuntimeCoordinator.CommandItem(
                runtime = fixture.runtime,
                listEntry = fixture.commandEntry(alias = "c"),
                resultId = fixture.resultId
            )
        )

        val results = fixture.useCase("calc").first().results

        val commandResult = assertIs<SearchResultSet.CommandSearchResult>(results.first())
        assertEquals(null, commandResult.listEntry.alias)
    }
}

private class SearchUseCaseFixture(
    includeCommands: Boolean,
    includeLive: Boolean
) {
    val pluginId = PluginId("ru.raydroid.calculator")
    val resultId = SearchResultId(
        pluginId = pluginId,
        commandName = "calculator",
        itemId = CommandItemId.CommandRoot
    )
    val runtime = SearchUseCaseRuntime(
        manifest = searchUseCaseManifest(pluginId, "calculator")
    )
    val commands = MutableStateFlow<List<PluginRuntimeCoordinator.CommandItem>>(emptyList())
    val content = MutableStateFlow<List<PluginRuntimeCoordinator.ContentItem>>(emptyList())
    val cachedItems = MutableStateFlow<Map<PluginRuntime, List<SearchIndexMutation>>>(emptyMap())
    val searchIndexRepository = SearchUseCaseSearchIndexRepository()
    val aliasRepository = SearchUseCaseAliasRepository()
    private val registry = SearchUseCaseRegistry(
        coordinator = SearchUseCaseCoordinator(
            commands = commands,
            content = content,
            cachedItems = cachedItems
        )
    )
    val useCase = SearchUseCase(
        pluginRuntimeRegistry = registry,
        searchIndexRepository = searchIndexRepository,
        searchResultRanker = SearchUseCaseRanker(
            includeCommands = includeCommands,
            includeLive = includeLive
        ),
        searchAliasRepository = aliasRepository
    )

    fun commandEntry(alias: String? = null): PluginCommandListItem {
        return PluginCommandListItem(
            id = CommandItemId.CommandRoot,
            icon = null,
            title = PluginUiText.Plain("Open YouTube"),
            description = PluginUiText.Plain("Launch browser"),
            alias = alias
        )
    }
}

private class SearchUseCaseCoordinator(
    private val commands: StateFlow<List<PluginRuntimeCoordinator.CommandItem>>,
    private val content: StateFlow<List<PluginRuntimeCoordinator.ContentItem>>,
    private val cachedItems: Flow<Map<PluginRuntime, List<SearchIndexMutation>>>
) : PluginRuntimeCoordinator {
    override fun cachedItems(): Flow<Map<PluginRuntime, List<SearchIndexMutation>>> = cachedItems
    override fun runtimes(): StateFlow<List<PluginRuntime>> = MutableStateFlow(emptyList())
    override fun content(): StateFlow<List<PluginRuntimeCoordinator.ContentItem>> = content
    override fun commands(): StateFlow<List<PluginRuntimeCoordinator.CommandItem>> = commands
    override suspend fun update(action: CommandActionBridge) = Unit
}

private class SearchUseCaseRegistry(
    private val coordinator: PluginRuntimeCoordinator
) : PluginRuntimeRegistry {
    override suspend fun load(runtime: PluginRuntime) = Unit
    override suspend fun unload(runtime: PluginRuntime) = Unit
    override fun get(): PluginRuntimeCoordinator = coordinator
}

private class SearchUseCaseRuntime(
    override val manifest: Manifest
) : PluginRuntime {
    override val pluginId: PluginId = PluginId(manifest.name)
    override val resources: FileSystem = FakeFileSystem()
    override fun cachedItems(chunkSize: Int): Flow<List<SearchIndexMutation>> = emptyFlow()
    override fun content(): StateFlow<List<PluginRuntime.ContentItem>> = MutableStateFlow(emptyList())
    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> = MutableStateFlow(null)
    override suspend fun actions(commandName: String, itemId: CommandItemId) = emptyList<ru.raydroid.plugin.host.api.ui.PluginCommandListAction>()
    override suspend fun update(action: CommandActionBridge) = Unit
    override suspend fun update(commandName: String, action: CommandActionBridge) = Unit
    override suspend fun back(commandName: String): Boolean = false
    override suspend fun unload() = Unit
}

private class SearchUseCaseSearchIndexRepository : SearchIndexRepository {
    val results = MutableStateFlow<List<RankedSearchResult>>(emptyList())
    var preview: RankedSearchResult? = null

    override suspend fun update(mutations: List<SearchIndexMutation>) = Unit
    override suspend fun updateUsage(resultId: SearchResultId) = Unit
    override suspend fun getPreview(resultId: SearchResultId): RankedSearchResult? = preview
    override fun search(query: String, limit: Int): Flow<List<RankedSearchResult>> = results
}

private class SearchUseCaseAliasRepository : SearchAliasRepository {
    val aliases = MutableStateFlow<Map<SearchResultId, String>>(emptyMap())

    override fun observeAliases(): Flow<Map<SearchResultId, String>> = aliases

    override suspend fun saveAlias(resultId: SearchResultId, alias: String): SearchAliasSaveResult {
        aliases.value = aliases.value + (resultId to alias)
        return SearchAliasSaveResult.Success(SearchAliasEntry(resultId, alias))
    }

    override suspend fun removeAlias(resultId: SearchResultId) {
        aliases.value = aliases.value - resultId
    }

    override suspend fun resolveExactAlias(alias: String): SearchResultId? {
        return aliases.value.entries.firstOrNull { entry -> entry.value == alias }?.key
    }
}

private class SearchUseCaseRanker(
    private val includeCommands: Boolean,
    private val includeLive: Boolean
) : SearchResultRanker {
    override fun rankLive(
        query: String,
        contentSnapshot: List<PluginRuntimeCoordinator.ContentItem>,
        limit: Int
    ): List<RankedSearchResult> {
        if (!includeLive) return emptyList()
        return contentSnapshot.map { item ->
            RankedSearchResult(
                result = SearchResultSet.LiveSearchResult(
                    resultId = item.resultId,
                    listEntry = item.listEntry,
                    presentation = item.presentation
                ),
                score = SearchResultScore(textScore = 1.0, live = true)
            )
        }
    }

    override fun rankCommands(
        query: String,
        commandsSnapshot: List<PluginRuntimeCoordinator.CommandItem>,
        limit: Int
    ): List<RankedSearchResult> {
        if (!includeCommands) return emptyList()
        return commandsSnapshot.map { item ->
            RankedSearchResult(
                result = SearchResultSet.CommandSearchResult(
                    resultId = item.resultId,
                    listEntry = item.listEntry
                ),
                score = SearchResultScore(textScore = 1.0)
            )
        }
    }

    override fun merge(
        commandResults: List<RankedSearchResult>,
        liveResults: List<RankedSearchResult>,
        cachedResults: List<RankedSearchResult>,
        limit: Int
    ): List<SearchResultSet.SearchResult> {
        return (liveResults + commandResults + cachedResults).take(limit).map { it.result }
    }
}

private fun searchUseCaseManifest(
    pluginId: PluginId,
    commandName: String
): Manifest {
    return Manifest(
        name = pluginId.id,
        title = UiText.Plain("Calculator"),
        description = UiText.Plain("Calculator extension"),
        author = UiText.Plain("Raydroid"),
        version = 1,
        platforms = listOf(Platform.IOS),
        categories = emptyList(),
        license = "MIT",
        commands = listOf(
            Command(
                service = commandName,
                title = UiText.Plain("Calculator"),
                description = UiText.Plain("Calculator extension"),
                placeholder = null,
                icon = null,
                mode = Command.Mode.View,
                match = null,
                arguments = emptyList(),
                preferences = emptyList()
            )
        ),
        resources = emptyMap()
    )
}
