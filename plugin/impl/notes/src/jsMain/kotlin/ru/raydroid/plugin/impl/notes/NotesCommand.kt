package ru.raydroid.plugin.impl.notes

import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.ActionPanelHintMode
import ru.raydroid.plugin.api.ui.EmptyViewData
import ru.raydroid.plugin.api.ui.Form
import ru.raydroid.plugin.api.ui.FormSubmitStyle
import ru.raydroid.plugin.api.ui.FormValue
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.api.ui.Column

class NotesCommand : CommandService() {
    private var notes = NotesState()
    private var selectedNoteId: CommandItemId? = null
    private var loaded = false

    override suspend fun cachedItems(requestedItems: List<CommandItemId>?, chunkSize: Int) = flow<List<CommandListItem>> {
        ensureLoaded()
        invalidateCache()
    }

    override fun CommandListScope.content() {
    }

    override fun RayScope.fullscreen() {
        val selectedNote = selectedNoteId
            ?.value
            ?.let { id -> notes.items.firstOrNull { note -> note.id == id } }
        if (selectedNote != null) {
            EditNoteForm(selectedNote)
            return
        }

        Column(spacing = Spacing.Medium) {
            Form(
                navigationTitle = UiText.Plain("New note"),
                actionPanelHintMode = ActionPanelHintMode.MenuOnly
            ) {
                textField(
                    id = TitleFieldId,
                    title = UiText.Plain("Title"),
                    placeholder = UiText.Plain("Note title"),
                    defaultValue = "",
                    required = true
                )
                textArea(
                    id = BodyFieldId,
                    title = UiText.Plain("Body"),
                    placeholder = UiText.Plain("Write something..."),
                    defaultValue = ""
                )
                submit(
                    title = UiText.Plain("Save"),
                    icon = Icon.Builtin("Save"),
                    style = FormSubmitStyle.Tonal
                ) { values ->
                    val title = values.text(TitleFieldId).trim()
                    if (title.isNotEmpty()) {
                        val body = values.text(BodyFieldId)
                        createNote(title, body)
                        Host.notification.showToast("Note saved")
                    }
                }
            }

            LazyList(searchBarPlaceholder = UiText.Plain("Filter notes")) {
                notes.items.forEach { note ->
                    item(
                        id = CommandItemId(note.id),
                        title = UiText.Plain(note.title),
                        subtitle = UiText.Plain(note.bodyPreview()),
                        icon = NoteItemIcon,
                        keywords = note.keywords(),
                        modifier = Modifier.actions {
                            noteActions(note)
                        },
                        detailMarkdown = note.markdown()
                    )
                }
                emptyView(
                    title = UiText.Plain("No notes yet"),
                    description = UiText.Plain("Create your first note from the form above."),
                    icon = NoteItemIcon
                )
            }
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        val note = notes.items.firstOrNull { note -> note.id == target.itemId.value } ?: return
        noteActions(note)
    }

    override suspend fun execute(action: CommandAction) {
        ensureLoaded()
        when (action) {
            is CommandAction.OpenCommand -> {
                selectedNoteId = null
                render()
                renderFullscreen()
            }
            is CommandAction.CloseCommand -> {
                selectedNoteId = null
            }
            is CommandAction.Enter -> {
                selectedNoteId = action.hoveredId
                renderFullscreen()
            }
            is CommandAction.Type -> {
                render()
            }
            is CommandAction.Focus -> {
                Unit
            }
        }
    }

    override suspend fun back(): Boolean {
        ensureLoaded()
        if (selectedNoteId == null) {
            return false
        }
        selectedNoteId = null
        renderFullscreen()
        return true
    }

    private fun CommandActionScope.noteActions(note: Note) {
        action(
            title = UiText.Plain("Edit"),
            icon = Icon.Builtin("Edit"),
            primary = true
        ) {
            selectedNoteId = CommandItemId(note.id)
            renderFullscreen()
        }
        action(
            title = UiText.Plain("Delete"),
            icon = Icon.Builtin("Delete"),
            style = CommandListAction.Style.Destructive
        ) {
            deleteNote(note.id)
            Host.notification.showToast("Note deleted")
        }
    }

    private fun RayScope.EditNoteForm(note: Note) {
        Form(
            navigationTitle = UiText.Plain("Edit note"),
            requireChanges = true,
            unchangedView = EmptyViewData(
                title = UiText.Plain("Ready to edit"),
                description = UiText.Plain("Change the title or body, then press Edit."),
                icon = NoteItemIcon
            ),
            actionPanelHintMode = ActionPanelHintMode.Hidden
        ) {
            textField(
                id = "$TitleFieldId-${note.id}",
                title = UiText.Plain("Title"),
                placeholder = UiText.Plain("Note title"),
                defaultValue = note.title,
                required = true
            )
            textArea(
                id = "$BodyFieldId-${note.id}",
                title = UiText.Plain("Body"),
                placeholder = UiText.Plain("Write something..."),
                defaultValue = note.body
            )
            submit(
                title = UiText.Plain("Edit"),
                icon = Icon.Builtin("Save"),
                style = FormSubmitStyle.Tonal
            ) { values ->
                val title = values.text("$TitleFieldId-${note.id}").trim()
                if (title.isNotEmpty()) {
                    updateNote(
                        noteId = note.id,
                        title = title,
                        body = values.text("$BodyFieldId-${note.id}")
                    )
                    Host.notification.showToast("Note updated")
                }
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        notes = Host.storage[StorageKey, NotesState.serializer()] ?: NotesState()
        loaded = true
    }

    private suspend fun createNote(title: String, body: String) {
        ensureLoaded()
        val note = Note(
            id = nextNoteId(title, body),
            title = title,
            body = body
        )
        notes = notes.copy(items = listOf(note) + notes.items)
        persist()
        selectedNoteId = CommandItemId(note.id)
        render()
        renderFullscreen()
    }

    private suspend fun deleteNote(noteId: String) {
        ensureLoaded()
        notes = notes.copy(items = notes.items.filterNot { note -> note.id == noteId })
        if (selectedNoteId?.value == noteId) {
            selectedNoteId = null
        }
        persist()
        render()
        renderFullscreen()
    }

    private suspend fun updateNote(
        noteId: String,
        title: String,
        body: String
    ) {
        ensureLoaded()
        notes = notes.copy(
            items = notes.items.map { note ->
                if (note.id == noteId) {
                    note.copy(title = title, body = body)
                } else {
                    note
                }
            }
        )
        persist()
        selectedNoteId = CommandItemId(noteId)
        renderFullscreen()
    }

    private suspend fun persist() {
        Host.storage[StorageKey, NotesState.serializer()] = notes
    }

    private fun nextNoteId(title: String, body: String): String {
        val base = title
            .lowercase()
            .map { char ->
                when {
                    char.isLetterOrDigit() -> char
                    else -> '-'
                }
            }
            .joinToString("")
            .trim('-')
            .take(48)
            .ifBlank { "note" }
        val usedIds = notes.items.map { note -> note.id }.toSet()
        var index = 1
        var candidate = "$base-${stableHash("$title\n$body")}"
        while (candidate in usedIds) {
            index++
            candidate = "$base-${stableHash("$title\n$body\n$index")}"
        }
        return candidate
    }

    private companion object {
        const val StorageKey = "notes-state"
        const val TitleFieldId = "title"
        const val BodyFieldId = "body"
        const val MaxTitleLength = 80
        val NotesIcon = Icon.Resource("icons/notes.png")
        val NoteItemIcon = Icon.Resource("icons/note-item.png")
    }
}

@Serializable
private data class NotesState(
    val items: List<Note> = emptyList()
)

@Serializable
private data class Note(
    val id: String,
    val title: String,
    val body: String
)

private fun Note.bodyPreview(): String =
    body.lineSequence()
        .map { line -> line.trim() }
        .firstOrNull { line -> line.isNotEmpty() }
        ?.take(120)
        ?: "Empty note"

private fun Note.markdown(): String =
    buildString {
        append("# ")
        appendLine(title.ifBlank { "Untitled" })
        appendLine()
        append(body.ifBlank { "_Empty note_" })
    }

private fun Note.keywords(): List<String> =
    (title.splitWords() + body.splitWords()).distinct().take(16)

private fun String.splitWords(): List<String> =
    split(Regex("\\s+")).map { word -> word.trim() }.filter { word -> word.length > 2 }

private fun Map<String, FormValue>.text(id: String): String =
    (get(id) as? FormValue.Text)?.value.orEmpty()

private fun stableHash(value: String): String {
    var hash = 5381
    value.forEach { char ->
        hash = ((hash shl 5) + hash) xor char.code
    }
    return hash.toUInt().toString(36)
}
