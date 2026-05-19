package ru.raydroid.plugin.host.impl.data.search

import ru.raydroid.plugin.api.manifest.Resources
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.host.api.domain.model.PluginId
import ru.raydroid.plugin.host.api.domain.model.RankedSearchResult
import ru.raydroid.plugin.host.api.domain.model.SearchResultId
import ru.raydroid.plugin.host.api.domain.model.SearchResultScore
import ru.raydroid.plugin.host.api.domain.model.SearchResultSet
import ru.raydroid.plugin.host.api.domain.runtime.PluginRuntimeCoordinator
import ru.raydroid.plugin.host.api.domain.service.SearchResultRanker
import ru.raydroid.plugin.host.api.ui.PluginUiText
import ru.raydroid.plugin.host.impl.data.search.cache.SearchIndexCacheSearchEntity
import ru.raydroid.plugin.host.impl.resource.resolveStringVariants
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

internal interface SearchRanker {
    fun rankCached(
        query: SearchQuery,
        ftsCandidates: List<SearchIndexCacheSearchEntity>,
        fallbackCandidates: List<SearchIndexCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<RankedSearchResult>

    fun rankRecent(
        candidates: List<SearchIndexCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<RankedSearchResult>
}

internal class CachedSearchRanker : SearchRanker, SearchResultRanker {
    override fun rankCached(
        query: SearchQuery,
        ftsCandidates: List<SearchIndexCacheSearchEntity>,
        fallbackCandidates: List<SearchIndexCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<RankedSearchResult> {
        val desiredCandidates = min(max(limit * RANKING_MULTIPLIER, limit), MAX_CANDIDATES)
        val candidates = selectCandidates(
            primaryCandidates = ftsCandidates,
            fallbackCandidates = fallbackCandidates,
            desiredCandidates = desiredCandidates
        )
        val bestByItem = linkedMapOf<SearchItemKey, ScoredCachedCandidate>()

        candidates.forEach { candidate ->
            val score = scoreCachedCandidate(query = query, candidate = candidate, nowEpochMs = nowEpochMs)
            if (!score.score.matched) return@forEach

            val key = SearchItemKey(
                pluginId = candidate.pluginId,
                commandName = candidate.command,
                itemId = candidate.itemId
            )
            val previous = bestByItem[key]
            if (previous == null || rankedComparator.compare(score.ranked, previous.ranked) < 0) {
                bestByItem[key] = score
            }
        }

        return bestByItem.values
            .map { it.ranked }
            .sortedWith(rankedComparator)
            .take(limit)
    }

    override fun rankRecent(
        candidates: List<SearchIndexCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<RankedSearchResult> {
        return candidates
            .distinctBy { candidate -> SearchItemKey(candidate.pluginId, candidate.command, candidate.itemId) }
            .mapIndexed { index, candidate ->
                RankedSearchResult(
                    result = candidate.toCachedSearchResult(emptyList(), emptyList()),
                    score = SearchResultScore(
                        textScore = 0.0,
                        usageBoost = usageBoost(
                            lastUsedAtEpochMs = candidate.lastUsedAtEpochMs,
                            usageCount = candidate.usageCount,
                            nowEpochMs = nowEpochMs
                        ),
                        stableOrder = index.toLong()
                    )
                )
            }
            .sortedWith(rankedComparator)
            .take(limit)
    }

    override fun rankLive(
        query: String,
        contentSnapshot: List<PluginRuntimeCoordinator.ContentItem>,
        limit: Int
    ): List<RankedSearchResult> {
        val normalizedQuery = SearchQueryNormalizer.from(query)
        if (contentSnapshot.isEmpty()) return emptyList()

        return contentSnapshot
            .mapIndexedNotNull { index, contentItem ->
                val command = contentItem.runtime.manifest.commands
                    .firstOrNull { command -> command.service == contentItem.resultId.commandName }
                val commandMatch = command?.match
                val regexBoost = if (commandMatch == null) {
                    0.0
                } else {
                    matchRegexBoost(match = commandMatch, rawQuery = query)
                        ?: return@mapIndexedNotNull null
                }
                val resources = contentItem.runtime.manifest.resources
                val pluginTexts = contentItem.runtime.manifest.title.resolve(resources)
                val commandTitleTexts = command?.title.resolve(resources)
                val commandDescriptionTexts = command?.description.resolve(resources)
                val titleTexts = contentItem.listEntry.title.resolve(resources)
                val descriptionTexts = contentItem.listEntry.description.resolve(resources)
                val fields = buildList {
                    titleTexts.forEach { add(SearchFieldInput(it, FieldWeight.LiveTitle)) }
                    commandTitleTexts.forEach { add(SearchFieldInput(it, FieldWeight.CommandTitle)) }
                    pluginTexts.forEach { add(SearchFieldInput(it, FieldWeight.PluginTitle)) }
                    commandDescriptionTexts.forEach { add(SearchFieldInput(it, FieldWeight.CommandDescription)) }
                    descriptionTexts.forEach { add(SearchFieldInput(it, FieldWeight.LiveDescription)) }
                    add(SearchFieldInput(contentItem.resultId.commandName, FieldWeight.CommandName))
                    add(SearchFieldInput(contentItem.resultId.pluginId.id, FieldWeight.PluginId))
                }
                val scored = scoreFields(
                    query = normalizedQuery,
                    fields = fields,
                    usageBoost = LIVE_SOURCE_BOOST + regexBoost,
                    live = true,
                    stableOrder = index.toLong()
                )
                val score: SearchResultScore? = if (!scored.matched && regexBoost > 0.0) {
                    SearchResultScore(
                        textScore = LIVE_REGEX_BOOST,
                        usageBoost = LIVE_SOURCE_BOOST,
                        live = true,
                        titleMatch = true,
                        stableOrder = index.toLong()
                    )
                } else if (!scored.matched && normalizedQuery.isBlank) {
                    SearchResultScore(
                        textScore = LIVE_EMPTY_QUERY_SCORE + regexBoost,
                        usageBoost = LIVE_SOURCE_BOOST,
                        live = true,
                        stableOrder = index.toLong()
                    )
                } else if (scored.matched) {
                    scored.toSearchResultScore(live = true, stableOrder = index.toLong())
                } else {
                    null
                }
                score?.let { liveScore ->
                    RankedSearchResult(
                        result = SearchResultSet.LiveSearchResult(
                            resultId = contentItem.resultId,
                            listEntry = contentItem.listEntry,
                            presentation = contentItem.presentation
                        ),
                        score = liveScore
                    )
                }
            }
            .sortedWith(rankedComparator)
            .take(limit)
    }

    override fun rankCommands(
        query: String,
        commandsSnapshot: List<PluginRuntimeCoordinator.CommandItem>,
        limit: Int
    ): List<RankedSearchResult> {
        val normalizedQuery = SearchQueryNormalizer.from(query)
        return commandsSnapshot
            .mapIndexedNotNull { index, commandItem ->
                val command = commandItem.runtime.manifest.commands
                    .firstOrNull { command -> command.service == commandItem.resultId.commandName }
                    ?: return@mapIndexedNotNull null
                val commandMatch = command.match
                val regexBoost = if (commandMatch == null) {
                    0.0
                } else {
                    matchRegexBoost(match = commandMatch, rawQuery = query) ?: 0.0
                }
                val resources = commandItem.runtime.manifest.resources
                val fields = buildList {
                    commandItem.listEntry.title.resolve(resources).forEach { add(SearchFieldInput(it, FieldWeight.LiveTitle)) }
                    commandItem.listEntry.description.resolve(resources).forEach {
                        add(SearchFieldInput(it, FieldWeight.LiveDescription))
                    }
                    command.title.resolve(resources).forEach { add(SearchFieldInput(it, FieldWeight.CommandTitle)) }
                    command.description.resolve(resources).forEach { add(SearchFieldInput(it, FieldWeight.CommandDescription)) }
                    commandItem.runtime.manifest.title.resolve(resources).forEach { add(SearchFieldInput(it, FieldWeight.PluginTitle)) }
                    add(SearchFieldInput(command.service, FieldWeight.CommandName))
                    add(SearchFieldInput(commandItem.resultId.pluginId.id, FieldWeight.PluginId))
                }
                val score = if (normalizedQuery.isBlank) {
                    SearchResultScore(
                        textScore = COMMAND_EMPTY_QUERY_SCORE,
                        usageBoost = COMMAND_SOURCE_BOOST,
                        live = true,
                        titleMatch = true,
                        stableOrder = index.toLong()
                    )
                } else {
                    val scored = scoreFields(
                        query = normalizedQuery,
                        fields = fields,
                        usageBoost = COMMAND_SOURCE_BOOST + regexBoost,
                        live = true,
                        stableOrder = index.toLong()
                    )
                    if (scored.matched) {
                        scored.toSearchResultScore(live = true, stableOrder = index.toLong())
                    } else if (commandItem.listEntry.trailingText != null && regexBoost > 0.0) {
                        SearchResultScore(
                            textScore = LIVE_REGEX_BOOST,
                            usageBoost = COMMAND_SOURCE_BOOST + regexBoost,
                            live = true,
                            titleMatch = true,
                            stableOrder = index.toLong()
                        )
                    } else {
                        return@mapIndexedNotNull null
                    }
                }
                val resultScore = if (commandItem.listEntry.trailingText != null) {
                    score.copy(
                        textScore = score.textScore + INLINE_RESULT_BOOST,
                        inlineResult = true,
                        prefix = true,
                        titleMatch = true
                    )
                } else {
                    score
                }

                RankedSearchResult(
                    result = SearchResultSet.CommandSearchResult(
                        resultId = commandItem.resultId,
                        listEntry = commandItem.listEntry
                    ),
                    score = resultScore
                )
            }
            .sortedWith(rankedComparator)
            .take(limit)
    }

    override fun merge(
        commandResults: List<RankedSearchResult>,
        liveResults: List<RankedSearchResult>,
        cachedResults: List<RankedSearchResult>,
        limit: Int
    ): List<SearchResultSet.SearchResult> {
        val bestById = linkedMapOf<SearchResultId, RankedSearchResult>()
        (commandResults + liveResults + cachedResults).forEach { candidate ->
            val previous = bestById[candidate.result.resultId]
            bestById[candidate.result.resultId] = if (previous == null) {
                candidate
            } else {
                mergeDuplicateResult(previous = previous, candidate = candidate)
            }
        }

        return bestById.values
            .sortedWith(rankedComparator)
            .take(limit)
            .map { it.result }
    }

    private fun mergeDuplicateResult(
        previous: RankedSearchResult,
        candidate: RankedSearchResult
    ): RankedSearchResult {
        val bestScore = if (rankedComparator.compare(candidate, previous) < 0) {
            candidate.score
        } else {
            previous.score
        }
        val preferredResult = when {
            candidate.result is SearchResultSet.LiveSearchResult && previous.result !is SearchResultSet.LiveSearchResult -> {
                candidate.result
            }
            previous.result is SearchResultSet.LiveSearchResult && candidate.result !is SearchResultSet.LiveSearchResult -> {
                previous.result
            }
            candidate.result is SearchResultSet.CommandSearchResult && previous.result is SearchResultSet.CachedSearchResult -> {
                candidate.result
            }
            previous.result is SearchResultSet.CommandSearchResult && candidate.result is SearchResultSet.CachedSearchResult -> {
                previous.result
            }
            rankedComparator.compare(candidate, previous) < 0 -> candidate.result
            else -> previous.result
        }

        return RankedSearchResult(
            result = preferredResult,
            score = bestScore.copy(
                live = preferredResult !is SearchResultSet.CachedSearchResult || bestScore.live
            )
        )
    }

    private fun scoreCachedCandidate(
        query: SearchQuery,
        candidate: SearchIndexCacheSearchEntity,
        nowEpochMs: Long
    ): ScoredCachedCandidate {
        val scored = scoreFields(
            query = query,
            fields = listOf(
                SearchFieldInput(candidate.title, FieldWeight.CachedTitle),
                SearchFieldInput(candidate.description, FieldWeight.CachedDescription)
            ),
            usageBoost = usageBoost(
                lastUsedAtEpochMs = candidate.lastUsedAtEpochMs,
                usageCount = candidate.usageCount,
                nowEpochMs = nowEpochMs
            ),
            live = false,
            stableOrder = candidate.contentId
        )
        val ranked = RankedSearchResult(
            result = candidate.toCachedSearchResult(
                titleMatches = scored.matches[FieldWeight.CachedTitle].orEmpty(),
                descriptionMatches = scored.matches[FieldWeight.CachedDescription].orEmpty()
            ),
            score = scored.toSearchResultScore(live = false, stableOrder = candidate.contentId)
        )
        return ScoredCachedCandidate(score = scored, ranked = ranked)
    }

    private fun scoreFields(
        query: SearchQuery,
        fields: List<SearchFieldInput>,
        usageBoost: Double,
        live: Boolean,
        stableOrder: Long
    ): ScoredFields {
        if (query.isBlank) {
            return ScoredFields.Unmatched
        }

        val matches = linkedMapOf<FieldWeight, List<IntRange>>()
        val best = fields
            .mapNotNull { input ->
                scoreField(query = query, rawText = input.text)
                    .takeIf { it.matched }
                    ?.let { match ->
                        matches[input.weight] = match.ranges
                        match.copy(
                            baseScore = match.baseScore + input.weight.boost,
                            weightTitleMatch = input.weight.titleLike
                        )
                    }
            }
            .minWithOrNull(fieldComparator)

        return if (best == null) {
            ScoredFields.Unmatched
        } else {
            ScoredFields(
                matched = true,
                textScore = best.baseScore,
                editDistance = best.editDistance,
                usageBoost = usageBoost,
                exact = best.exact,
                prefix = best.prefix,
                titleMatch = best.weightTitleMatch,
                fieldLength = best.fieldLength,
                matches = matches,
                live = live,
                stableOrder = stableOrder
            )
        }
    }

    private fun scoreField(query: SearchQuery, rawText: String?): FieldMatch {
        if (rawText.isNullOrEmpty()) return FieldMatch.Unmatched
        val field = NormalizedText.from(rawText)
        if (field.text.isEmpty()) return FieldMatch.Unmatched

        val acronym = SearchQueryNormalizer.acronym(field)
        if (query.normalized.text.length >= 2 && acronym.startsWith(query.normalized.text)) {
            return FieldMatch(
                matched = true,
                baseScore = if (acronym == query.normalized.text) 185.0 else 172.0,
                editDistance = 0,
                ranges = acronymRanges(field, query.normalized.text.length),
                fieldLength = field.text.length,
                exact = acronym == query.normalized.text,
                prefix = true,
                matchedAllTokens = true
            )
        }

        val exactIndex = field.text.indexOf(query.normalized.text)
        if (exactIndex >= 0) {
            val range = field.toOriginalRanges(listOf(exactIndex..(exactIndex + query.normalized.text.lastIndex)))
            return FieldMatch(
                matched = true,
                baseScore = when {
                    field.text == query.normalized.text -> 240.0
                    exactIndex == 0 -> 210.0
                    else -> 170.0
                },
                editDistance = 0,
                ranges = range,
                fieldLength = field.text.length,
                exact = field.text == query.normalized.text,
                prefix = exactIndex == 0
            )
        }

        val tokenMatch = scoreTokenCoverage(query = query, field = field)
        if (tokenMatch.matchedAllTokens) return tokenMatch

        val subsequenceMatch = scoreOrderedSubsequence(query = query, field = field)
        if (subsequenceMatch != null) return subsequenceMatch

        val bestWindow = bestAlignmentWindow(query = query, field = field)
        if (bestWindow != null && !query.isShort) {
            return FieldMatch(
                matched = true,
                baseScore = 118.0 - bestWindow.distance * 10.0 + bestWindow.coverageBonus,
                editDistance = bestWindow.distance,
                ranges = field.toOriginalRanges(bestWindow.ranges),
                fieldLength = field.text.length,
                exact = false,
                prefix = bestWindow.startsAtZero
            )
        }

        return tokenMatch.takeIf { it.matched } ?: FieldMatch.Unmatched
    }

    private fun scoreOrderedSubsequence(query: SearchQuery, field: NormalizedText): FieldMatch? {
        if (query.normalized.text.length < 2 || field.text.isEmpty()) return null

        val matchedPositions = mutableListOf<Int>()
        var searchFrom = 0
        query.normalized.text.forEach { queryChar ->
            val nextIndex = field.text.indexOf(queryChar, startIndex = searchFrom)
            if (nextIndex < 0) return null
            matchedPositions += nextIndex
            searchFrom = nextIndex + 1
        }

        val gapPenalty = matchedPositions
            .zipWithNext { left, right -> max(0, right - left - 1) }
            .sum()
        val startPenalty = matchedPositions.firstOrNull() ?: 0
        val coverageBonus = query.normalized.text.length.toDouble() / field.text.length.coerceAtLeast(1)
        val baseScore = 136.0 +
            coverageBonus * 12.0 -
            gapPenalty * 3.0 -
            startPenalty * 2.0

        return FieldMatch(
            matched = true,
            baseScore = baseScore,
            editDistance = gapPenalty,
            ranges = field.toOriginalRanges(mergeRanges(matchedPositions.map { it..it })),
            fieldLength = field.text.length,
            exact = false,
            prefix = matchedPositions.firstOrNull() == 0
        )
    }

    private fun scoreTokenCoverage(query: SearchQuery, field: NormalizedText): FieldMatch {
        if (query.normalized.tokens.isEmpty() || field.tokenRanges.isEmpty()) return FieldMatch.Unmatched

        val ranges = mutableListOf<IntRange>()
        var matchedTokens = 0
        var prefixMatches = 0

        query.normalized.tokens.forEach { queryToken ->
            val match = field.tokenRanges.firstNotNullOfOrNull { tokenRange ->
                val tokenText = field.text.substring(range = tokenRange)
                when {
                    tokenText == queryToken -> tokenRange to true
                    tokenText.startsWith(queryToken) -> tokenRange.first..(tokenRange.first + queryToken.lastIndex) to true
                    queryToken.length >= 2 && tokenText.contains(queryToken) -> {
                        val startIndex = tokenText.indexOf(queryToken)
                        (tokenRange.first + startIndex)..(tokenRange.first + startIndex + queryToken.lastIndex) to false
                    }
                    else -> null
                }
            } ?: return@forEach

            matchedTokens += 1
            if (match.second) prefixMatches += 1
            ranges += match.first
        }

        if (matchedTokens == 0) return FieldMatch.Unmatched
        val matchedAllTokens = matchedTokens == query.normalized.tokens.size
        return FieldMatch(
            matched = true,
            baseScore = if (matchedAllTokens) {
                158.0 + matchedTokens * 4.0 + prefixMatches * 3.0
            } else {
                78.0 + matchedTokens * 3.0 + prefixMatches * 2.0
            },
            editDistance = max(query.normalized.tokens.size - matchedTokens, 0),
            ranges = field.toOriginalRanges(mergeRanges(ranges)),
            fieldLength = field.text.length,
            exact = false,
            prefix = prefixMatches > 0,
            matchedAllTokens = matchedAllTokens
        )
    }

    private fun bestAlignmentWindow(query: SearchQuery, field: NormalizedText): WindowAlignment? {
        if (query.normalized.text.length < 3 || field.text.isEmpty()) return null
        val maxDistance = max(1, query.normalized.text.length / 3)
        var bestAlignment: WindowAlignment? = null

        candidateWindows(field = field, queryTokenCount = query.normalized.tokens.size).forEach { window ->
            val text = field.text.substring(range = window)
            val alignment = editDistanceAlignment(
                query = query.normalized.text,
                candidate = text,
                maxDistance = maxDistance
            ) ?: return@forEach

            val ranges = alignment.ranges.map { range ->
                (range.first + window.first)..(range.last + window.first)
            }
            val current = WindowAlignment(
                distance = alignment.distance,
                coverageBonus = alignment.coverageRatio * 10.0,
                ranges = mergeRanges(ranges),
                startsAtZero = window.first == 0
            )
            if (bestAlignment == null || windowComparator.compare(current, bestAlignment) < 0) {
                bestAlignment = current
            }
        }

        return bestAlignment
    }

    private fun candidateWindows(field: NormalizedText, queryTokenCount: Int): List<IntRange> {
        if (field.tokenRanges.isEmpty()) return listOf(0..field.text.lastIndex)

        val tokenCount = max(queryTokenCount, 1)
        val windowSizes = linkedSetOf(max(1, tokenCount - 1), tokenCount, tokenCount + 1)
        val windows = linkedSetOf<IntRange>()

        windowSizes.forEach { windowSize ->
            if (windowSize > field.tokenRanges.size) return@forEach
            for (startIndex in 0..(field.tokenRanges.size - windowSize)) {
                val start = field.tokenRanges[startIndex].first
                val end = field.tokenRanges[startIndex + windowSize - 1].last
                windows += start..end
            }
        }

        if (field.tokenRanges.size <= tokenCount + 2) {
            windows += field.tokenRanges.first().first..field.tokenRanges.last().last
        }

        return windows.toList()
    }

    private fun editDistanceAlignment(query: String, candidate: String, maxDistance: Int): AlignmentResult? {
        if (abs(query.length - candidate.length) > maxDistance) return null

        val rows = query.length + 1
        val cols = candidate.length + 1
        val distances = IntArray(rows * cols)
        val directions = ByteArray(rows * cols)

        fun index(row: Int, col: Int) = row * cols + col

        for (row in 0..query.length) {
            distances[index(row, 0)] = row
            if (row > 0) directions[index(row, 0)] = DELETE
        }
        for (col in 0..candidate.length) {
            distances[index(0, col)] = col
            if (col > 0) directions[index(0, col)] = INSERT
        }

        for (row in 1..query.length) {
            var rowMinimum = Int.MAX_VALUE
            for (col in 1..candidate.length) {
                val substitutionCost = if (query[row - 1] == candidate[col - 1]) 0 else 1
                val deleteCost = distances[index(row - 1, col)] + 1
                val insertCost = distances[index(row, col - 1)] + 1
                val substituteCost = distances[index(row - 1, col - 1)] + substitutionCost

                val bestCost = min(substituteCost, min(deleteCost, insertCost))
                distances[index(row, col)] = bestCost
                directions[index(row, col)] = when (bestCost) {
                    substituteCost -> DIAGONAL
                    insertCost -> INSERT
                    else -> DELETE
                }
                rowMinimum = min(rowMinimum, bestCost)
            }
            if (rowMinimum > maxDistance) return null
        }

        val finalDistance = distances[index(query.length, candidate.length)]
        if (finalDistance > maxDistance) return null

        val matchedPositions = mutableListOf<Int>()
        var row = query.length
        var col = candidate.length
        while (row > 0 || col > 0) {
            when (directions[index(row, col)]) {
                DIAGONAL -> {
                    row -= 1
                    col -= 1
                    matchedPositions += col
                }
                INSERT -> {
                    col -= 1
                    matchedPositions += col
                }
                else -> row -= 1
            }
        }

        if (matchedPositions.isEmpty()) return null
        matchedPositions.sort()
        return AlignmentResult(
            distance = finalDistance,
            coverageRatio = matchedPositions.size.toDouble() / candidate.length.coerceAtLeast(1),
            ranges = mergeRanges(matchedPositions.map { it..it })
        )
    }

    private fun matchRegexBoost(match: String, rawQuery: String): Double? {
        return runCatching {
            if (Regex(match).containsMatchIn(rawQuery)) LIVE_REGEX_BOOST else null
        }.getOrElse { 0.0 }
    }

    private fun usageBoost(lastUsedAtEpochMs: Long?, usageCount: Long, nowEpochMs: Long): Double {
        if (lastUsedAtEpochMs == null) return 0.0
        val ageMs = (nowEpochMs - lastUsedAtEpochMs).coerceAtLeast(0)
        val recencyScore = 0.25 * exp(-ageMs / SEVEN_DAYS_MS)
        val frequencyScore = min(0.1, ln((usageCount + 1).toDouble()) / 20.0)
        return min(0.35, recencyScore + frequencyScore)
    }

    private fun selectCandidates(
        primaryCandidates: List<SearchIndexCacheSearchEntity>,
        fallbackCandidates: List<SearchIndexCacheSearchEntity>,
        desiredCandidates: Int
    ): List<SearchIndexCacheSearchEntity> {
        val selected = ArrayList<SearchIndexCacheSearchEntity>(desiredCandidates)
        val seenContentIds = LinkedHashSet<Long>(desiredCandidates)

        primaryCandidates.forEach { candidate ->
            if (seenContentIds.add(candidate.contentId)) selected += candidate
            if (selected.size >= desiredCandidates) return selected
        }

        fallbackCandidates.forEach { candidate ->
            if (seenContentIds.add(candidate.contentId)) selected += candidate
            if (selected.size >= desiredCandidates) return selected
        }

        return selected
    }

    private fun SearchIndexCacheSearchEntity.toCachedSearchResult(
        titleMatches: List<IntRange>,
        descriptionMatches: List<IntRange>
    ): SearchResultSet.CachedSearchResult {
        return SearchResultSet.CachedSearchResult(
            resultId = SearchResultId(
                pluginId = PluginId(pluginId),
                commandName = command,
                itemId = CommandItemId(itemId)
            ),
            listEntry = toPluginListEntry(),
            titleMatches = titleMatches,
            descriptionMatches = descriptionMatches
        )
    }

    private fun ScoredFields.toSearchResultScore(live: Boolean, stableOrder: Long): SearchResultScore {
        return SearchResultScore(
            textScore = textScore,
            editDistance = editDistance,
            usageBoost = usageBoost,
            exact = exact,
            prefix = prefix,
            titleMatch = titleMatch,
            live = live,
            fieldLength = fieldLength,
            stableOrder = stableOrder
        )
    }

    private fun UiText?.resolve(resources: Resources): List<String> {
        return when (this?.type) {
            null -> emptyList()
            UiText.Type.Plain -> listOf(text)
            UiText.Type.Resource -> resolveStringVariants(resources, text).values.distinct()
        }
    }

    private fun PluginUiText?.resolve(resources: Resources): List<String> {
        return when (this) {
            null -> emptyList()
            is PluginUiText.Plain -> listOf(text)
            is PluginUiText.Resource -> resolveStringVariants(resources, key).values.distinct()
        }
    }

    private fun acronymRanges(field: NormalizedText, length: Int): List<IntRange> {
        return field.tokenRanges
            .take(length)
            .map { tokenRange -> field.toOriginalRanges(listOf(tokenRange.first..tokenRange.first)) }
            .flatten()
    }

    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        var current = sorted.first()

        for (index in 1 until sorted.size) {
            val next = sorted[index]
            current = if (next.first <= current.last + 1) {
                current.first..max(current.last, next.last)
            } else {
                merged += current
                next
            }
        }

        merged += current
        return merged
    }

    private data class SearchFieldInput(
        val text: String?,
        val weight: FieldWeight
    )

    private enum class FieldWeight(val boost: Double, val titleLike: Boolean = false) {
        CachedTitle(28.0, titleLike = true),
        LiveTitle(30.0, titleLike = true),
        CommandTitle(22.0, titleLike = true),
        PluginTitle(14.0, titleLike = true),
        CommandName(10.0, titleLike = true),
        CachedDescription(0.0),
        LiveDescription(0.0),
        CommandDescription(0.0),
        PluginId(-12.0)
    }

    private data class SearchItemKey(
        val pluginId: String,
        val commandName: String,
        val itemId: String
    )

    private data class WindowAlignment(
        val distance: Int,
        val coverageBonus: Double,
        val ranges: List<IntRange>,
        val startsAtZero: Boolean
    )

    private data class AlignmentResult(
        val distance: Int,
        val coverageRatio: Double,
        val ranges: List<IntRange>
    )

    private data class ScoredCachedCandidate(
        val score: ScoredFields,
        val ranked: RankedSearchResult
    )

    private data class ScoredFields(
        val matched: Boolean,
        val textScore: Double,
        val editDistance: Int,
        val usageBoost: Double,
        val exact: Boolean,
        val prefix: Boolean,
        val titleMatch: Boolean,
        val fieldLength: Int,
        val matches: Map<FieldWeight, List<IntRange>>,
        val live: Boolean,
        val stableOrder: Long
    ) {
        companion object {
            val Unmatched = ScoredFields(
                matched = false,
                textScore = Double.NEGATIVE_INFINITY,
                editDistance = Int.MAX_VALUE,
                usageBoost = 0.0,
                exact = false,
                prefix = false,
                titleMatch = false,
                fieldLength = Int.MAX_VALUE,
                matches = emptyMap(),
                live = false,
                stableOrder = Long.MAX_VALUE
            )
        }
    }

    private data class FieldMatch(
        val matched: Boolean,
        val baseScore: Double,
        val editDistance: Int,
        val ranges: List<IntRange>,
        val fieldLength: Int,
        val exact: Boolean,
        val prefix: Boolean,
        val matchedAllTokens: Boolean = false,
        val weightTitleMatch: Boolean = false
    ) {
        companion object {
            val Unmatched = FieldMatch(
                matched = false,
                baseScore = Double.NEGATIVE_INFINITY,
                editDistance = Int.MAX_VALUE,
                ranges = emptyList(),
                fieldLength = Int.MAX_VALUE,
                exact = false,
                prefix = false
            )
        }
    }

    private companion object {
        const val RANKING_MULTIPLIER = 4
        const val MAX_CANDIDATES = 192
        const val SEVEN_DAYS_MS = 7.0 * 24 * 60 * 60 * 1000
        const val LIVE_SOURCE_BOOST = 0.2
        const val LIVE_REGEX_BOOST = 48.0
        const val LIVE_EMPTY_QUERY_SCORE = 4.0
        const val COMMAND_SOURCE_BOOST = 0.3
        const val COMMAND_EMPTY_QUERY_SCORE = 6.0
        const val INLINE_RESULT_BOOST = 96.0

        const val DELETE: Byte = 1
        const val INSERT: Byte = 2
        const val DIAGONAL: Byte = 3
    }

    private val fieldComparator = compareByDescending<FieldMatch> { it.baseScore }
        .thenBy { it.editDistance }
        .thenBy { it.fieldLength }

    private val windowComparator = compareBy<WindowAlignment>(
        { it.distance },
        { -it.coverageBonus },
        { !it.startsAtZero }
    )

    private val rankedComparator = compareByDescending<RankedSearchResult> { it.score.exact }
        .thenByDescending { it.score.prefix }
        .thenByDescending { it.score.inlineResult }
        .thenByDescending { it.score.textScore }
        .thenBy { it.score.editDistance }
        .thenByDescending { it.score.usageBoost }
        .thenByDescending { it.score.live }
        .thenByDescending { it.score.titleMatch }
        .thenBy { it.score.fieldLength }
        .thenBy { it.score.stableOrder }
}

private fun String.substring(range: IntRange): String {
    return substring(range.first, range.last + 1)
}
