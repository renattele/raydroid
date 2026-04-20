package ru.raydroid.plugin.host.api.domain.model

data class RankedSearchResult(
    val result: SearchResultSet.SearchResult,
    val score: SearchResultScore
)

data class SearchResultScore(
    val textScore: Double,
    val editDistance: Int = Int.MAX_VALUE,
    val usageBoost: Double = 0.0,
    val exact: Boolean = false,
    val prefix: Boolean = false,
    val titleMatch: Boolean = false,
    val live: Boolean = false,
    val fieldLength: Int = Int.MAX_VALUE,
    val stableOrder: Long = 0
)
