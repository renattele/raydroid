package ru.raydroid.plugin.host.impl.data.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.SearchAliasEntry
import ru.raydroid.plugin.host.api.domain.model.SearchAliasNormalizationResult
import ru.raydroid.plugin.host.api.domain.model.SearchAliasNormalizer
import ru.raydroid.plugin.host.api.domain.model.SearchAliasSaveResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.repository.SearchAliasRepository

internal class SearchAliasRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : SearchAliasRepository {
    override fun observeAliases(): Flow<Map<SearchResultId, String>> {
        return callbackFlow {
            val job = launch {
                dataStore.data.collect { preferences ->
                    val storedValue = preferences[AliasesKey]
                    val document = decode(storedValue)
                    val canonical = document.canonicalize()
                    if (storedValue != canonical.encode()) {
                        dataStore.updateData { latest ->
                            latest.toMutablePreferences().also { mutablePreferences ->
                                mutablePreferences.writeDocument(
                                    decode(latest[AliasesKey]).canonicalize()
                                )
                            }
                        }
                    }
                    trySend(canonical.toAliasMap())
                }
            }
            awaitClose { job.cancel() }
        }
    }

    override suspend fun saveAlias(
        resultId: SearchResultId,
        alias: String
    ): SearchAliasSaveResult {
        return when (val normalized = SearchAliasNormalizer.normalize(alias)) {
            is SearchAliasNormalizationResult.Invalid -> SearchAliasSaveResult.Invalid(normalized.reason)
            is SearchAliasNormalizationResult.Valid -> {
                var saveResult: SearchAliasSaveResult? = null
                dataStore.updateData { preferences ->
                    val document = decode(preferences[AliasesKey]).canonicalize()
                    val conflict = document.entries.firstOrNull { entry ->
                        entry.alias == normalized.normalized && entry.resultId() != resultId
                    }
                    val nextDocument = if (conflict != null) {
                        saveResult = SearchAliasSaveResult.Conflict(
                            alias = conflict.alias,
                            existingResultId = conflict.resultId()
                        )
                        document
                    } else {
                        saveResult = SearchAliasSaveResult.Success(
                            SearchAliasEntry(
                                resultId = resultId,
                                alias = normalized.normalized
                            )
                        )
                        SearchAliasDocument(
                            entries = document.entries
                                .filterNot { entry -> entry.resultId() == resultId }
                                .plus(SearchAliasRecord.from(resultId, normalized.normalized))
                                .sortedBy { entry -> entry.alias }
                        )
                    }
                    preferences.toMutablePreferences().also { mutablePreferences ->
                        mutablePreferences.writeDocument(nextDocument)
                    }
                }
                checkNotNull(saveResult)
            }
        }
    }

    override suspend fun removeAlias(resultId: SearchResultId) {
        dataStore.updateData { preferences ->
            val updatedEntries = decode(preferences[AliasesKey]).canonicalize().entries
                .filterNot { entry -> entry.resultId() == resultId }
            preferences.toMutablePreferences().also { mutablePreferences ->
                mutablePreferences.writeDocument(SearchAliasDocument(updatedEntries))
            }
        }
    }

    override suspend fun resolveExactAlias(alias: String): SearchResultId? {
        val normalized = SearchAliasNormalizer.normalizeLookup(alias) ?: return null
        return decode(dataStore.data.first()[AliasesKey]).canonicalize().entries
            .firstOrNull { entry -> entry.alias == normalized }
            ?.resultId()
    }

    private fun decode(value: String?): SearchAliasDocument {
        if (value.isNullOrBlank()) return SearchAliasDocument()
        return runCatching {
            val root = json.parseToJsonElement(value).jsonObject
            val entries = root["entries"]
                ?.jsonArray
                ?.mapNotNull { item ->
                    item.jsonObject.toSearchAliasRecordOrNull()
                }
                .orEmpty()
            SearchAliasDocument(entries)
        }.getOrDefault(SearchAliasDocument())
    }

    private fun SearchAliasDocument.toAliasMap(): Map<SearchResultId, String> {
        return entries.mapNotNull { entry ->
            runCatching {
                entry.resultId() to entry.alias
            }.getOrNull()
        }.toMap()
    }

    private fun MutablePreferences.writeDocument(document: SearchAliasDocument) {
        this[AliasesKey] = document.encode()
    }

    private companion object {
        val AliasesKey = stringPreferencesKey("search_aliases")
    }
}

private data class SearchAliasDocument(
    val entries: List<SearchAliasRecord> = emptyList()
) {
    fun encode(): String {
        val root = buildJsonObject {
            put(
                "entries",
                buildJsonArray {
                    entries.forEach { entry ->
                        add(
                            buildJsonObject {
                                put("pluginId", JsonPrimitive(entry.pluginId))
                                put("commandName", JsonPrimitive(entry.commandName))
                                put("itemId", JsonPrimitive(entry.itemId))
                                put("alias", JsonPrimitive(entry.alias))
                            }
                        )
                    }
                }
            )
        }
        return root.toString()
    }

    fun canonicalize(): SearchAliasDocument {
        val seenResultIds = linkedSetOf<SearchResultId>()
        val seenAliases = linkedSetOf<String>()
        val canonicalEntries = buildList {
            entries.forEach { entry ->
                val resultId = runCatching { entry.resultId() }.getOrNull() ?: return@forEach
                val normalizedAlias = SearchAliasNormalizer.normalizeLookup(entry.alias) ?: return@forEach
                if (!seenResultIds.add(resultId) || !seenAliases.add(normalizedAlias)) return@forEach
                add(SearchAliasRecord.from(resultId, normalizedAlias))
            }
        }.sortedBy { entry -> entry.alias }
        return SearchAliasDocument(canonicalEntries)
    }
}

private data class SearchAliasRecord(
    val pluginId: String,
    val commandName: String,
    val itemId: String,
    val alias: String
) {
    fun resultId(): SearchResultId {
        return SearchResultId(
            pluginId = PluginId(pluginId),
            commandName = commandName,
            itemId = CommandItemId(itemId)
        )
    }

    companion object {
        fun from(resultId: SearchResultId, alias: String): SearchAliasRecord {
            return SearchAliasRecord(
                pluginId = resultId.pluginId.id,
                commandName = resultId.commandName,
                itemId = resultId.itemId.value,
                alias = alias
            )
        }
    }
}

private fun JsonObject.toSearchAliasRecordOrNull(): SearchAliasRecord? {
    val pluginId = get("pluginId")?.jsonPrimitive?.contentOrNull ?: return null
    val commandName = get("commandName")?.jsonPrimitive?.contentOrNull ?: return null
    val itemId = get("itemId")?.jsonPrimitive?.contentOrNull ?: return null
    val alias = get("alias")?.jsonPrimitive?.contentOrNull ?: return null
    return SearchAliasRecord(
        pluginId = pluginId,
        commandName = commandName,
        itemId = itemId,
        alias = alias
    )
}
