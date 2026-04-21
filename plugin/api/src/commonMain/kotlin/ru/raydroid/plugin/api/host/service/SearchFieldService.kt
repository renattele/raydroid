package ru.raydroid.plugin.api.host.service

import kotlinx.serialization.Serializable

interface SearchFieldService {
    suspend fun setState(state: SearchFieldState)

    suspend fun setText(text: String) {
        setState(SearchFieldState(text))
    }

    suspend fun clear() {
        setText("")
    }
}

@Serializable
data class SearchFieldState(
    val text: String,
    val selection: SearchFieldSelection = SearchFieldSelection.CursorAtEnd
)

@Serializable
enum class SearchFieldSelection {
    CursorAtStart,
    CursorAtEnd,
    SelectAll
}
