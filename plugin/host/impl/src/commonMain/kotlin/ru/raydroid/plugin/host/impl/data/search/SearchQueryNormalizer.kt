package ru.raydroid.plugin.host.impl.data.search

internal object SearchQueryNormalizer {
    fun from(raw: String): SearchQuery {
        val normalized = NormalizedText.from(raw)
        val ftsTokens = normalized.tokens.filter { token -> token.length >= MIN_FTS_TOKEN_LENGTH }
        val acronym = acronym(normalized)
        val relaxedTokens =
            (ftsTokens + acronym.takeIf { it.length >= MIN_FTS_TOKEN_LENGTH })
                .filterNotNull()
                .distinct()
        return SearchQuery(
            raw = raw,
            normalized = normalized,
            acronym = acronym,
            strictFtsQuery = ftsTokens.takeIf { it.isNotEmpty() }?.joinToString(" AND ") { token -> "$token*" },
            relaxedFtsQuery = relaxedTokens.takeIf { it.size > 1 }?.joinToString(" OR ") { token -> "$token*" },
            isShort = normalized.text.length < MIN_FUZZY_QUERY_LENGTH,
            isBlank = normalized.text.isBlank(),
        )
    }

    fun searchable(raw: String?): String = raw?.let { NormalizedText.from(it).text }.orEmpty()

    fun acronym(raw: String?): String = acronym(NormalizedText.from(raw.orEmpty()))

    fun acronym(text: NormalizedText): String =
        text.tokenRanges
            .mapNotNull { range -> text.text.getOrNull(range.first) }
            .joinToString("")

    private const val MIN_FTS_TOKEN_LENGTH = 2
    private const val MIN_FUZZY_QUERY_LENGTH = 3
}

internal data class SearchQuery(
    val raw: String,
    val normalized: NormalizedText,
    val acronym: String,
    val strictFtsQuery: String?,
    val relaxedFtsQuery: String?,
    val isShort: Boolean,
    val isBlank: Boolean,
)

internal data class NormalizedText(
    val text: String,
    val originalIndices: List<Int>,
    val tokens: List<String>,
    val tokenRanges: List<IntRange>,
) {
    fun toOriginalRanges(ranges: List<IntRange>): List<IntRange> =
        ranges.mapNotNull { range ->
            if (range.first !in originalIndices.indices || range.last !in originalIndices.indices) {
                null
            } else {
                originalIndices[range.first]..originalIndices[range.last]
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

                    else -> {
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
                tokens = tokenRanges.map { range -> text.substring(range.first, range.last + 1) },
                tokenRanges = tokenRanges,
            )
        }
    }
}
