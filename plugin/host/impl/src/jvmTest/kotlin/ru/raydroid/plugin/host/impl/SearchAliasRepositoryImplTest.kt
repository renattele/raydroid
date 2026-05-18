package ru.raydroid.plugin.host.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchAliasInvalidReason
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.impl.data.search.SearchAliasRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchAliasRepositoryImplTest {
    @Test
    fun `save normalizes alias and resolve trims lookup`() = runTest {
        val repository = repository(testScheduler)
        val resultId = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)

        val result = repository.saveAlias(resultId, "  OY  ")

        val success = assertIs<SearchAliasSaveResult.Success>(result)
        assertEquals("oy", success.entry.alias)
        assertEquals(mapOf(resultId to "oy"), repository.observeAliases().first())
        assertEquals(resultId, repository.resolveExactAlias(" oy "))
    }

    @Test
    fun `save rejects duplicates owned by another target`() = runTest {
        val repository = repository(testScheduler)
        val first = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)
        val second = resultId("ru.raydroid.notes", "notes", CommandItemId.CommandRoot)
        repository.saveAlias(first, "oy")

        val result = repository.saveAlias(second, "oy")

        val conflict = assertIs<SearchAliasSaveResult.Conflict>(result)
        assertEquals(first, conflict.existingResultId)
        assertEquals(mapOf(first to "oy"), repository.observeAliases().first())
    }

    @Test
    fun `concurrent duplicate saves keep a single winner`() = runTest {
        val repository = repository(testScheduler)
        val first = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)
        val second = resultId("ru.raydroid.notes", "notes", CommandItemId.CommandRoot)

        val firstSave = async { repository.saveAlias(first, "oy") }
        val secondSave = async { repository.saveAlias(second, "oy") }
        advanceUntilIdle()

        val results = listOf(firstSave.await(), secondSave.await())
        assertEquals(1, results.count { it is SearchAliasSaveResult.Success })
        assertEquals(1, results.count { it is SearchAliasSaveResult.Conflict })
        assertEquals(1, repository.observeAliases().first().size)
    }

    @Test
    fun `save rejects aliases with whitespace`() = runTest {
        val repository = repository(testScheduler)
        val result = repository.saveAlias(
            resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot),
            "open youtube"
        )

        val invalid = assertIs<SearchAliasSaveResult.Invalid>(result)
        assertEquals(SearchAliasInvalidReason.ContainsWhitespace, invalid.reason)
    }

    @Test
    fun `save rejects non ascii aliases`() = runTest {
        val repository = repository(testScheduler)
        val result = repository.saveAlias(
            resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot),
            "с"
        )

        val invalid = assertIs<SearchAliasSaveResult.Invalid>(result)
        assertEquals(SearchAliasInvalidReason.NonAscii, invalid.reason)
    }

    @Test
    fun `remove clears stored alias`() = runTest {
        val repository = repository(testScheduler)
        val resultId = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)
        repository.saveAlias(resultId, "oy")

        repository.removeAlias(resultId)

        assertEquals(emptyMap(), repository.observeAliases().first())
    }

    @Test
    fun `observe canonicalizes duplicate aliases already stored`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val store = PreferenceDataStoreFactory.createWithPath(scope = scope) {
            buildString {
                append("/tmp/search-aliases-")
                append(testScheduler.currentTime)
                append("-")
                append(hashCode())
                append(".preferences_pb")
            }.toPath()
        }
        val first = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)
        val second = resultId("ru.raydroid.notes", "notes", CommandItemId.CommandRoot)
        store.updateData { preferences ->
            preferences.toMutablePreferences().also { mutable ->
                mutable[stringPreferencesKey("search_aliases")] = """
                    {"entries":[
                        {"pluginId":"${first.pluginId.id}","commandName":"${first.commandName}","itemId":"${first.itemId.value}","alias":"oy"},
                        {"pluginId":"${second.pluginId.id}","commandName":"${second.commandName}","itemId":"${second.itemId.value}","alias":"oy"}
                    ]}
                """.trimIndent()
            }
        }
        val repository = SearchAliasRepositoryImpl(
            dataStore = store,
            json = Json { ignoreUnknownKeys = true }
        )

        val aliases = repository.observeAliases().first()

        assertEquals(mapOf(first to "oy"), aliases)
        assertEquals(first, repository.resolveExactAlias("oy"))
    }

    @Test
    fun `observe removes non ascii aliases that visually match ascii aliases`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        val store = PreferenceDataStoreFactory.createWithPath(scope = scope) {
            buildString {
                append("/tmp/search-aliases-")
                append(testScheduler.currentTime)
                append("-")
                append(hashCode())
                append(".preferences_pb")
            }.toPath()
        }
        val latin = resultId("ru.raydroid.calculator", "calculator", CommandItemId.CommandRoot)
        val cyrillic = resultId("ru.raydroid.apps", "apps", CommandItemId("com.apple.clock"))
        store.updateData { preferences ->
            preferences.toMutablePreferences().also { mutable ->
                mutable[stringPreferencesKey("search_aliases")] = """
                    {"entries":[
                        {"pluginId":"${latin.pluginId.id}","commandName":"${latin.commandName}","itemId":"${latin.itemId.value}","alias":"c"},
                        {"pluginId":"${cyrillic.pluginId.id}","commandName":"${cyrillic.commandName}","itemId":"${cyrillic.itemId.value}","alias":"с"}
                    ]}
                """.trimIndent()
            }
        }
        val repository = SearchAliasRepositoryImpl(
            dataStore = store,
            json = Json { ignoreUnknownKeys = true }
        )

        val aliases = repository.observeAliases().first()

        assertEquals(mapOf(latin to "c"), aliases)
        assertEquals(null, repository.resolveExactAlias("с"))
    }

    private fun repository(scheduler: TestCoroutineScheduler): SearchAliasRepositoryImpl {
        val scope = CoroutineScope(StandardTestDispatcher(scheduler) + SupervisorJob())
        val store = PreferenceDataStoreFactory.createWithPath(scope = scope) {
            buildString {
                append("/tmp/search-aliases-")
                append(scheduler.currentTime)
                append("-")
                append(hashCode())
                append(".preferences_pb")
            }.toPath()
        }
        return SearchAliasRepositoryImpl(
            dataStore = store,
            json = Json { ignoreUnknownKeys = true }
        )
    }

    private fun resultId(
        pluginId: String,
        commandName: String,
        itemId: CommandItemId
    ): SearchResultId {
        return SearchResultId(
            pluginId = PluginId(pluginId),
            commandName = commandName,
            itemId = itemId
        )
    }
}
