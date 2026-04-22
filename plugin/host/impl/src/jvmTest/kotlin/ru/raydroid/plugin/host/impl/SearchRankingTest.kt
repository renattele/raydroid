package ru.raydroid.plugin.host.impl

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandPresentation
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchIndexMutation
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntime
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.ui.PluginCommandListAction
import ru.raydroid.plugin.host.api.ui.PluginCommandListItem
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.data.search.CachedSearchRanker
import ru.raydroid.plugin.host.impl.data.search.SearchQueryNormalizer
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheContentEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheSearchEntity
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheWithContent
import ru.raydroid.plugin.host.impl.data.search.cache.getAppDatabase
import ru.raydroid.plugin.host.impl.data.search.cache.getDatabaseBuilder
import ru.raydroid.plugin.host.impl.ui.toPluginCommandListItem
import ru.raydroid.plugin.host.impl.ui.toPluginCommandPresentation

class SearchRankingTest {
    private val ranker = CachedSearchRanker()

    @Test
    fun `normalizer strips punctuation and builds acronym`() {
        val query = SearchQueryNormalizer.from("  Visual-Studio_Code!! ")

        assertEquals("visual studio code", query.normalized.text)
        assertEquals("vsc", query.acronym)
        assertEquals("visual* AND studio* AND code*", query.strictFtsQuery)
    }

    @Test
    fun `manifest command searchable defaults to true and accepts false`() {
        val defaultCommand = Json.decodeFromString<Command>(
            """
            {
              "service": "CalculatorCommand",
              "title": "command.title",
              "description": "command.description",
              "mode": "View",
              "match": null,
              "arguments": [],
              "preferences": []
            }
            """.trimIndent()
        )
        val hiddenCommand = Json.decodeFromString<Command>(
            """
            {
              "service": "CalculatorCommand",
              "title": "command.title",
              "description": "command.description",
              "placeholder": "command.placeholder",
              "mode": "View",
              "match": null,
              "searchable": false,
              "arguments": [],
              "preferences": []
            }
            """.trimIndent()
        )

        assertTrue(defaultCommand.searchable)
        assertNull(defaultCommand.placeholder)
        assertEquals(UiText.Resource("command.placeholder"), hiddenCommand.placeholder)
        assertFalse(hiddenCommand.searchable)
    }

    @Test
    fun `cached exact title beats fuzzy title`() {
        val ranked = ranker.rankCached(
            query = SearchQueryNormalizer.from("Calculator"),
            ftsCandidates = listOf(
                cachedCandidate(contentId = 1, itemId = "calc", title = "Calculator"),
                cachedCandidate(contentId = 2, itemId = "calendar", title = "Calendar")
            ),
            fallbackCandidates = emptyList(),
            limit = 10,
            nowEpochMs = 1_000
        )

        val first = assertIs<SearchResultSet.CachedSearchResult>(ranked.first().result)
        assertEquals("calc", first.resultId.itemId.value)
        assertTrue(first.titleMatches.isNotEmpty())
    }

    @Test
    fun `live command regex gates non matching content`() {
        val runtime = FakeSearchRuntime(
            manifest = manifest(match = "[0-9].*"),
            contentItems = listOf(contentItem(title = "Calculator"))
        )
        val content = coordinatorContent(runtime)

        assertTrue(ranker.rankLive("abc", content, limit = 10).isEmpty())
        assertTrue(ranker.rankLive("123", content, limit = 10).isNotEmpty())
    }

    @Test
    fun `merge keeps live result for duplicate result id`() {
        val runtime = FakeSearchRuntime(
            manifest = manifest(match = null),
            contentItems = listOf(contentItem(title = "Calculator"))
        )
        val live = ranker.rankLive("Calculator", coordinatorContent(runtime), limit = 10)
        val cached = ranker.rankCached(
            query = SearchQueryNormalizer.from("Calculator"),
            ftsCandidates = listOf(cachedCandidate(contentId = 1, itemId = CommandItemId.Static.value, title = "Calculator")),
            fallbackCandidates = emptyList(),
            limit = 10,
            nowEpochMs = 1_000
        )

        val merged = ranker.merge(commandResults = emptyList(), liveResults = live, cachedResults = cached, limit = 10)

        assertIs<SearchResultSet.LiveSearchResult>(merged.single())
    }

    @Test
    fun `command title is searchable even when live regex rejects query`() {
        val runtime = FakeSearchRuntime(
            manifest = manifest(match = "[0-9].*"),
            contentItems = emptyList()
        )

        val rankedCommands = ranker.rankCommands("Calculator", listOf(commandItem(runtime)), limit = 10)

        assertIs<SearchResultSet.CommandSearchResult>(rankedCommands.single().result)
        assertTrue(ranker.rankLive("Calculator", coordinatorContent(runtime), limit = 10).isEmpty())
    }

    @Test
    fun `room fts finds normalized prefix and acronym columns`() = runTest {
        val dbFile = File.createTempFile("raydroid-search", ".db").apply { delete() }
        val database = getAppDatabase(getDatabaseBuilder(dbFile.absolutePath.toPath()))
        val dao = database.getSearchIndexCacheDao()
        val titleSearch = SearchQueryNormalizer.searchable("Visual Studio Code")
        val descriptionSearch = SearchQueryNormalizer.searchable("Code editor")

        try {
            dao.insert(
                SearchIndexCacheWithContent(
                    searchIndexCache = SearchIndexCacheEntity(
                        pluginId = "ru.test.plugin",
                        command = "apps",
                        itemId = "vscode",
                        icon = null,
                        iconType = null
                    ),
                    content = listOf(
                        SearchIndexCacheContentEntity(
                            title = "Visual Studio Code",
                            description = "Code editor",
                            titleSearch = titleSearch,
                            descriptionSearch = descriptionSearch,
                            acronymSearch = SearchQueryNormalizer.acronym("Visual Studio Code")
                        )
                    )
                )
            )

            assertEquals("Visual Studio Code", dao.searchFtsCandidates("visual*", 10).first().single().title)
            assertEquals("Visual Studio Code", dao.searchFtsCandidates("vsc*", 10).first().single().title)
        } finally {
            database.close()
            dbFile.delete()
        }
    }

    private fun cachedCandidate(
        contentId: Long,
        itemId: String,
        title: String,
        description: String? = null
    ): SearchIndexCacheSearchEntity {
        return SearchIndexCacheSearchEntity(
            cacheId = contentId,
            contentId = contentId,
            pluginId = "ru.test.plugin",
            command = "apps",
            itemId = itemId,
            icon = null,
            iconType = null,
            title = title,
            description = description,
            lastUsedAtEpochMs = null,
            usageCount = 0
        )
    }

    private fun contentItem(title: String): PluginRuntime.ContentItem {
        val listEntry = CommandListItem(
            id = CommandItemId.Static,
            icon = null,
            title = UiText.Plain(title),
            description = UiText.Plain("Inline result")
        )
        return PluginRuntime.ContentItem(
            commandName = "apps",
            presentation = CommandPresentation(
                listEntry = listEntry,
                content = emptyList()
            )
        )
    }

    private fun coordinatorContent(runtime: PluginRuntime): List<PluginRuntimeCoordinator.ContentItem> {
        return runtime.content().value.map { contentItem ->
            PluginRuntimeCoordinator.ContentItem(
                runtime = runtime,
                presentation = contentItem.presentation.toPluginCommandPresentation(runtime.pluginId),
                listEntry = contentItem.presentation.listEntry.toPluginCommandListItem(runtime.pluginId),
                resultId = SearchResultId(
                    pluginId = runtime.pluginId,
                    commandName = contentItem.commandName,
                    itemId = contentItem.presentation.listEntry.id
                )
            )
        }
    }

    private fun commandItem(runtime: PluginRuntime): PluginRuntimeCoordinator.CommandItem {
        val command = runtime.manifest.commands.single()
        return PluginRuntimeCoordinator.CommandItem(
            runtime = runtime,
            listEntry = PluginCommandListItem(
                id = CommandItemId.CommandRoot,
                icon = null,
                title = command.title.toPluginText(runtime.pluginId),
                description = command.description.toPluginText(runtime.pluginId)
            ),
            resultId = SearchResultId(
                pluginId = runtime.pluginId,
                commandName = command.service,
                itemId = CommandItemId.CommandRoot
            )
        )
    }

    private fun manifest(match: String?): Manifest {
        return Manifest(
            name = "ru.test.plugin",
            title = UiText.Plain("Test Plugin"),
            description = UiText.Plain("Test plugin"),
            author = UiText.Plain("Codex"),
            version = 1,
            platforms = listOf(Platform.MacOS),
            categories = emptyList(),
            license = "MIT",
            commands = listOf(
                Command(
                    service = "apps",
                    title = UiText.Plain("Calculator"),
                    description = UiText.Plain("Calculator command"),
                    mode = Command.Mode.Inline,
                    match = match,
                    arguments = emptyList(),
                    preferences = emptyList()
                )
            ),
            resources = emptyMap()
        )
    }
}

private class FakeSearchRuntime(
    override val manifest: Manifest,
    contentItems: List<PluginRuntime.ContentItem>
) : PluginRuntime {
    override val pluginId: PluginId = PluginId(manifest.name)
    override val resources: FileSystem = FakeFileSystem()
    private val content = MutableStateFlow(contentItems)

    override fun cachedItems(chunkSize: Int) = emptyFlow<List<SearchIndexMutation>>()

    override fun content() = content

    override fun fullscreen(commandName: String): StateFlow<PluginRuntime.FullscreenContent?> = MutableStateFlow(null)

    override suspend fun actions(
        commandName: String,
        target: CommandActionTarget
    ): List<PluginCommandListAction> = emptyList()

    override suspend fun update(query: String, action: CommandAction) = Unit

    override suspend fun update(commandName: String, query: String, action: CommandAction) = Unit

    override suspend fun unload() = Unit
}

private fun UiText.toPluginText(pluginId: PluginId): PluginUiText = when (type) {
    UiText.Type.Plain -> PluginUiText.Plain(text)
    UiText.Type.Resource -> PluginUiText.Resource(pluginId = pluginId, key = text)
}
