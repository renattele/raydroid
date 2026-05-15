package ru.raydroid.plugin.impl.notes.model

import kotlinx.serialization.Serializable

@Serializable
internal data class NotesState(
    val items: List<Note> = emptyList()
)

@Serializable
internal data class Note(
    val id: String,
    val title: String,
    val body: String
)
