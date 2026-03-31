package ru.raydroid.plugin.host.impl

import ru.raydroid.plugin.host.api.ListItemId
import ru.raydroid.plugin.host.api.PluginId
import ru.raydroid.plugin.host.api.SearchResults
import ru.raydroid.plugin.host.impl.datasource.cache.ListItemCacheSearchEntity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

internal interface SearchRanker {
    fun rank(
        query: NormalizedText,
        ftsCandidates: List<ListItemCacheSearchEntity>,
        fallbackCandidates: List<ListItemCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<SearchResults.Item>
}

internal class CachedSearchRanker : SearchRanker {
    override fun rank(
        query: NormalizedText,
        ftsCandidates: List<ListItemCacheSearchEntity>,
        fallbackCandidates: List<ListItemCacheSearchEntity>,
        limit: Int,
        nowEpochMs: Long
    ): List<SearchResults.Item> {
        val desiredCandidates = min(max(limit * RANKING_MULTIPLIER, limit), MAX_CANDIDATES)
        val candidates = selectCandidates(
            ftsCandidates = ftsCandidates,
            fallbackCandidates = fallbackCandidates,
            desiredCandidates = desiredCandidates
        )
        val bestByItem = linkedMapOf<SearchItemKey, ScoredCandidate>()

        candidates.forEach { candidate ->
            val score = scoreCandidate(query = query, candidate = candidate, nowEpochMs = nowEpochMs)
            if (!score.matched) return@forEach

            val key = SearchItemKey(
                pluginId = candidate.pluginId,
                commandName = candidate.command,
                itemId = candidate.itemId
            )
            val previous = bestByItem[key]
            if (previous == null || scoredCandidateComparator.compare(score, previous) < 0) {
                bestByItem[key] = score
            }
        }

        return bestByItem.values
            .sortedWith(scoredCandidateComparator)
            .take(limit)
            .map { score ->
                SearchResults.Item(
                    listItemId = ListItemId(
                        pluginId = PluginId(score.candidate.pluginId),
                        commandName = score.candidate.command,
                        itemId = ru.raydroid.plugin.api.core.ItemId(score.candidate.itemId)
                    ),
                    item = score.candidate.toListItem(),
                    titleMatches = score.titleMatches,
                    descriptionMatches = score.descriptionMatches
                )
            }
    }

    private fun scoreCandidate(
        query: NormalizedText,
        candidate: ListItemCacheSearchEntity,
        nowEpochMs: Long
    ): ScoredCandidate {
        val titleMatch = scoreField(
            query = query,
            rawText = candidate.title
        )
        val descriptionMatch = scoreField(
            query = query,
            rawText = candidate.description
        )
        val bestField = listOf(titleMatch, descriptionMatch).minWithOrNull(fieldComparator)
            ?: FieldMatch.Unmatched
        val usageBoost = usageBoost(
            lastUsedAtEpochMs = candidate.lastUsedAtEpochMs,
            usageCount = candidate.usageCount,
            nowEpochMs = nowEpochMs
        )

        return ScoredCandidate(
            candidate = candidate,
            matched = bestField.matched,
            baseScore = bestField.baseScore,
            editDistance = bestField.editDistance,
            usageBoost = usageBoost,
            isTitleMatch = fieldComparator.compare(titleMatch, descriptionMatch) <= 0,
            fieldLength = bestField.fieldLength,
            exact = bestField.exact,
            prefix = bestField.prefix,
            titleMatches = titleMatch.ranges,
            descriptionMatches = descriptionMatch.ranges
        )
    }

    private fun scoreField(
        query: NormalizedText,
        rawText: String?
    ): FieldMatch {
        if (rawText.isNullOrEmpty()) return FieldMatch.Unmatched
        val field = NormalizedText.from(rawText)
        if (field.text.isEmpty()) return FieldMatch.Unmatched

        val exactIndex = field.text.indexOf(query.text)
        if (exactIndex >= 0) {
            val range = field.toOriginalRanges(listOf(exactIndex..(exactIndex + query.text.lastIndex)))
            return FieldMatch(
                matched = true,
                baseScore = when {
                    field.text == query.text -> 220.0
                    exactIndex == 0 -> 190.0
                    else -> 165.0
                },
                editDistance = 0,
                ranges = range,
                fieldLength = field.text.length,
                exact = field.text == query.text,
                prefix = exactIndex == 0
            )
        }

        val tokenMatch = scoreTokenCoverage(query = query, field = field)
        if (tokenMatch.matchedAllTokens) {
            return tokenMatch
        }

        val subsequenceMatch = scoreOrderedSubsequence(query = query, field = field)
        if (subsequenceMatch != null) {
            return subsequenceMatch
        }

        val bestWindow = bestAlignmentWindow(query = query, field = field)
        if (bestWindow != null) {
            return FieldMatch(
                matched = true,
                baseScore = 120.0 - bestWindow.distance * 10.0 + bestWindow.coverageBonus,
                editDistance = bestWindow.distance,
                ranges = field.toOriginalRanges(bestWindow.ranges),
                fieldLength = field.text.length,
                exact = false,
                prefix = bestWindow.startsAtZero
            )
        }

        return tokenMatch.takeIf { it.matched } ?: FieldMatch.Unmatched
    }

    private fun scoreOrderedSubsequence(
        query: NormalizedText,
        field: NormalizedText
    ): FieldMatch? {
        if (query.text.isEmpty() || field.text.isEmpty()) return null

        val matchedPositions = mutableListOf<Int>()
        var searchFrom = 0

        query.text.forEach { queryChar ->
            val nextIndex = field.text.indexOf(queryChar, startIndex = searchFrom)
            if (nextIndex < 0) return null
            matchedPositions += nextIndex
            searchFrom = nextIndex + 1
        }

        val gapPenalty = matchedPositions
            .zipWithNext { left, right -> max(0, right - left - 1) }
            .sum()
        val startPenalty = matchedPositions.firstOrNull() ?: 0
        val coverageBonus = query.text.length.toDouble() / field.text.length.coerceAtLeast(1)
        val baseScore = 140.0 +
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

    private fun scoreTokenCoverage(
        query: NormalizedText,
        field: NormalizedText
    ): FieldMatch {
        if (query.tokens.isEmpty() || field.tokenRanges.isEmpty()) return FieldMatch.Unmatched

        val ranges = mutableListOf<IntRange>()
        var matchedTokens = 0
        var prefixMatches = 0

        query.tokens.forEach { queryToken ->
            val match = field.tokenRanges.firstNotNullOfOrNull { tokenRange ->
                val tokenText = field.text.substring(tokenRange)
                when {
                    tokenText == queryToken -> tokenRange to true
                    tokenText.startsWith(queryToken) -> tokenRange.first..(tokenRange.first + queryToken.lastIndex) to true
                    tokenText.contains(queryToken) -> {
                        val startIndex = tokenText.indexOf(queryToken)
                        (tokenRange.first + startIndex)..(tokenRange.first + startIndex + queryToken.lastIndex) to false
                    }
                    else -> null
                }
            } ?: return@forEach

            matchedTokens += 1
            if (match.second) {
                prefixMatches += 1
            }
            ranges += match.first
        }

        if (matchedTokens == 0) return FieldMatch.Unmatched

        val matchedAllTokens = matchedTokens == query.tokens.size
        return FieldMatch(
            matched = true,
            baseScore = if (matchedAllTokens) {
                150.0 + matchedTokens * 4.0 + prefixMatches * 3.0
            } else {
                80.0 + matchedTokens * 3.0 + prefixMatches * 2.0
            },
            editDistance = max(query.tokens.size - matchedTokens, 0),
            ranges = field.toOriginalRanges(mergeRanges(ranges)),
            fieldLength = field.text.length,
            exact = false,
            prefix = prefixMatches > 0,
            matchedAllTokens = matchedAllTokens
        )
    }

    private fun bestAlignmentWindow(
        query: NormalizedText,
        field: NormalizedText
    ): WindowAlignment? {
        if (query.text.isEmpty() || field.text.isEmpty()) return null
        val maxDistance = max(1, query.text.length / 3)
        var bestAlignment: WindowAlignment? = null

        candidateWindows(field = field, queryTokenCount = query.tokens.size).forEach { window ->
            val text = field.text.substring(window)
            val alignment = editDistanceAlignment(
                query = query.text,
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

    private fun candidateWindows(
        field: NormalizedText,
        queryTokenCount: Int
    ): List<IntRange> {
        if (field.tokenRanges.isEmpty()) return listOf(0..field.text.lastIndex)

        val tokenCount = max(queryTokenCount, 1)
        val windowSizes = linkedSetOf(
            max(1, tokenCount - 1),
            tokenCount,
            tokenCount + 1
        )
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

    private fun editDistanceAlignment(
        query: String,
        candidate: String,
        maxDistance: Int
    ): AlignmentResult? {
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
                else -> {
                    row -= 1
                }
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

    private fun usageBoost(
        lastUsedAtEpochMs: Long?,
        usageCount: Long,
        nowEpochMs: Long
    ): Double {
        if (lastUsedAtEpochMs == null) return 0.0
        val ageMs = (nowEpochMs - lastUsedAtEpochMs).coerceAtLeast(0)
        val recencyScore = 0.25 * exp(-ageMs / SEVEN_DAYS_MS)
        val frequencyScore = min(0.1, ln((usageCount + 1).toDouble()) / 20.0)
        return min(0.35, recencyScore + frequencyScore)
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

    private data class ScoredCandidate(
        val candidate: ListItemCacheSearchEntity,
        val matched: Boolean,
        val baseScore: Double,
        val editDistance: Int,
        val usageBoost: Double,
        val isTitleMatch: Boolean,
        val fieldLength: Int,
        val exact: Boolean,
        val prefix: Boolean,
        val titleMatches: List<IntRange>,
        val descriptionMatches: List<IntRange>
    )

    private data class FieldMatch(
        val matched: Boolean,
        val baseScore: Double,
        val editDistance: Int,
        val ranges: List<IntRange>,
        val fieldLength: Int,
        val exact: Boolean,
        val prefix: Boolean,
        val matchedAllTokens: Boolean = false
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

    private fun selectCandidates(
        ftsCandidates: List<ListItemCacheSearchEntity>,
        fallbackCandidates: List<ListItemCacheSearchEntity>,
        desiredCandidates: Int
    ): List<ListItemCacheSearchEntity> {
        val selected = ArrayList<ListItemCacheSearchEntity>(desiredCandidates)
        val seenContentIds = LinkedHashSet<Long>(desiredCandidates)

        ftsCandidates.forEach { candidate ->
            if (seenContentIds.add(candidate.contentId)) {
                selected += candidate
            }
            if (selected.size >= desiredCandidates) {
                return selected
            }
        }

        if (selected.size < desiredCandidates) {
            fallbackCandidates.forEach { candidate ->
                if (seenContentIds.add(candidate.contentId)) {
                    selected += candidate
                }
                if (selected.size >= desiredCandidates) {
                    return selected
                }
            }
        }

        return selected
    }

    private companion object {
        const val RANKING_MULTIPLIER = 4
        const val MAX_CANDIDATES = 512
        const val SEVEN_DAYS_MS = 7.0 * 24 * 60 * 60 * 1000

        const val DELETE: Byte = 1
        const val INSERT: Byte = 2
        const val DIAGONAL: Byte = 3
    }

    private val fieldComparator = compareBy<FieldMatch>(
        { !it.matched },
        { !it.exact },
        { !it.prefix },
        { !it.matchedAllTokens },
        { -it.baseScore },
        { it.editDistance },
        { it.fieldLength }
    )

    private val windowComparator = compareBy<WindowAlignment>(
        { it.distance },
        { -it.coverageBonus },
        { !it.startsAtZero }
    )

    private val scoredCandidateComparator = compareBy<ScoredCandidate>(
        { !it.exact },
        { !it.prefix },
        { -it.baseScore },
        { it.editDistance },
        { -it.usageBoost },
        { !it.isTitleMatch },
        { it.fieldLength },
        { it.candidate.contentId }
    )
}

internal data class NormalizedText(
    val text: String,
    val originalIndices: List<Int>,
    val tokens: List<String>,
    val tokenRanges: List<IntRange>
) {
    fun toFtsMatchQuery(): String {
        return tokens.joinToString(" AND ") { "$it*" }
    }

    fun toOriginalRanges(ranges: List<IntRange>): List<IntRange> {
        return ranges.mapNotNull { range ->
            if (range.first !in originalIndices.indices || range.last !in originalIndices.indices) {
                null
            } else {
                originalIndices[range.first]..originalIndices[range.last]
            }
        }
    }

    companion object {
        fun from(raw: String): NormalizedText {
            val normalized = StringBuilder(raw.length)
            val originalIndices = mutableListOf<Int>()

            raw.forEachIndexed { index, char ->
                when {
                    char.isLetterOrDigit() -> {
                        normalized.append(char.lowercaseChar())
                        originalIndices += index
                    }
                    char.isWhitespace() -> {
                        if (normalized.isNotEmpty() && normalized.last() != ' ') {
                            normalized.append(' ')
                            originalIndices += index
                        }
                    }
                }
            }

            while (normalized.isNotEmpty() && normalized.first() == ' ') {
                normalized.deleteAt(0)
                originalIndices.removeAt(0)
            }
            while (normalized.isNotEmpty() && normalized.last() == ' ') {
                normalized.deleteAt(normalized.lastIndex)
                originalIndices.removeAt(originalIndices.lastIndex)
            }

            val text = normalized.toString()
            val tokenRanges = mutableListOf<IntRange>()
            var tokenStart = -1
            text.forEachIndexed { index, char ->
                if (char == ' ') {
                    if (tokenStart >= 0) {
                        tokenRanges += tokenStart until index
                        tokenStart = -1
                    }
                } else if (tokenStart < 0) {
                    tokenStart = index
                }
            }
            if (tokenStart >= 0) {
                tokenRanges += tokenStart..text.lastIndex
            }

            return NormalizedText(
                text = text,
                originalIndices = originalIndices,
                tokens = tokenRanges.map { range -> text.substring(range) },
                tokenRanges = tokenRanges
            )
        }
    }
}

private fun String.substring(range: IntRange): String {
    return substring(range.first, range.last + 1)
}

private fun ListItemCacheSearchEntity.toListItem(): ru.raydroid.plugin.api.core.ListItem {
    return ru.raydroid.plugin.api.core.ListItem(
        id = ru.raydroid.plugin.api.core.ItemId(itemId),
        icon = icon?.let { iconValue ->
            iconType?.let { ru.raydroid.plugin.api.ui.Icon(iconValue, ru.raydroid.plugin.api.ui.Icon.Type.valueOf(it)) }
        },
        title = title?.let(ru.raydroid.plugin.api.core.UiText::Plain),
        description = description?.let(ru.raydroid.plugin.api.core.UiText::Plain)
    )
}
