package ru.raydroid.plugin.host.api.domain.model

data class SearchAliasEntry(
    val resultId: SearchResultId,
    val alias: String
)

sealed interface SearchAliasSaveResult {
    data class Success(
        val entry: SearchAliasEntry
    ) : SearchAliasSaveResult

    data class Conflict(
        val alias: String,
        val existingResultId: SearchResultId
    ) : SearchAliasSaveResult

    data class Invalid(
        val reason: SearchAliasInvalidReason
    ) : SearchAliasSaveResult
}

enum class SearchAliasInvalidReason {
    Blank,
    ContainsWhitespace,
    NonAscii
}

object SearchAliasNormalizer {
    fun normalizeLookup(value: String): String? = normalize(value).normalized

    fun normalize(value: String): SearchAliasNormalizationResult {
        val normalized = value.trim().lowercase()
        return when {
            normalized.isBlank() -> SearchAliasNormalizationResult.Invalid(SearchAliasInvalidReason.Blank)
            normalized.any(Char::isWhitespace) -> SearchAliasNormalizationResult.Invalid(
                SearchAliasInvalidReason.ContainsWhitespace
            )
            normalized.any { char -> char.code !in 0x21..0x7E } -> SearchAliasNormalizationResult.Invalid(
                SearchAliasInvalidReason.NonAscii
            )

            else -> SearchAliasNormalizationResult.Valid(normalized)
        }
    }
}

sealed interface SearchAliasNormalizationResult {
    val normalized: String?

    data class Valid(
        override val normalized: String
    ) : SearchAliasNormalizationResult

    data class Invalid(
        val reason: SearchAliasInvalidReason
    ) : SearchAliasNormalizationResult {
        override val normalized: String? = null
    }
}
