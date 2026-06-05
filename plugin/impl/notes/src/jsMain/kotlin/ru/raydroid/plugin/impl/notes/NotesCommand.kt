package ru.raydroid.plugin.impl.notes

import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListAction
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.ActionPanelHintMode
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.EmptyViewData
import ru.raydroid.plugin.api.ui.Form
import ru.raydroid.plugin.api.ui.FormSubmitStyle
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.LazyList
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.actions
import ru.raydroid.plugin.impl.notes.model.Note
import ru.raydroid.plugin.impl.notes.model.NotesState
import ru.raydroid.plugin.impl.notes.util.bodyPreview
import ru.raydroid.plugin.impl.notes.util.keywords
import ru.raydroid.plugin.impl.notes.util.markdown
import ru.raydroid.plugin.impl.notes.util.noteIdBase
import ru.raydroid.plugin.impl.notes.util.stableHash
import ru.raydroid.plugin.impl.notes.util.text
import ru.raydroid.plugin.impl.notes.util.toCommandListItem

class NotesCommand : CommandService() {
    private var notes = NotesState()
    private var selectedNoteId: CommandItemId? = null
    private var loaded = false

    override suspend fun cachedItems(
        requestedItems: List<CommandItemId>?,
        chunkSize: Int,
    ) = flow {
        ensureLoaded()
        val requestedIds = requestedItems?.map { itemId -> itemId.value }?.toSet()
        notes.items
            .asSequence()
            .filter { note -> requestedIds == null || note.id in requestedIds }
            .map { note -> note.toCommandListItem(NoteSearchIcon) }
            .chunked(chunkSize)
            .forEach { chunk -> emit(chunk) }
    }

    override fun CommandListScope.content() {
    }

    override fun RayScope.fullscreen() {
        val selectedNote =
            selectedNoteId
                ?.value
                ?.let { id -> notes.items.firstOrNull { note -> note.id == id } }
        if (selectedNote != null) {
            editNoteForm(selectedNote)
            return
        }

        Column(spacing = Spacing.Medium) {
            Form(
                navigationTitle = UiText.Resource("notes.new.title"),
                actionPanelHintMode = ActionPanelHintMode.MenuOnly,
            ) {
                textField(
                    id = TITLE_FIELD_ID,
                    title = UiText.Resource("notes.field.title"),
                    placeholder = UiText.Resource("notes.field.title.placeholder"),
                    defaultValue = "",
                    required = true,
                )
                textArea(
                    id = BODY_FIELD_ID,
                    title = UiText.Resource("notes.field.body"),
                    placeholder = UiText.Resource("notes.field.body.placeholder"),
                    defaultValue = "",
                )
                submit(
                    title = UiText.Resource("notes.action.save"),
                    icon = Icon.Builtin("Save"),
                    style = FormSubmitStyle.Tonal,
                ) { values ->
                    val title = values.text(TITLE_FIELD_ID).trim()
                    if (title.isNotEmpty()) {
                        val body = values.text(BODY_FIELD_ID)
                        createNote(title, body)
                        Host.notification.showToast(UiText.Resource("notes.toast.saved"))
                    }
                }
            }

            LazyList(searchBarPlaceholder = UiText.Resource("notes.filter.placeholder")) {
                notes.items.forEach { note ->
                    item(
                        id = CommandItemId(note.id),
                        title = UiText.Plain(note.title),
                        subtitle = UiText.Plain(note.bodyPreview()),
                        icon = NoteListIcon,
                        iconColor = NoteMutedIconColor,
                        keywords = note.keywords(),
                        modifier =
                            Modifier.actions {
                                noteActions(note)
                            },
                        detailMarkdown = note.markdown(),
                    )
                }
                emptyView(
                    title = UiText.Resource("notes.empty.title"),
                    description = UiText.Resource("notes.empty.description"),
                    icon = NoteItemIcon,
                    iconColor = NoteMutedIconColor,
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

            is CommandAction.Focus -> {}
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
            title = UiText.Resource("notes.action.edit"),
            icon = Icon.Builtin("Edit"),
            primary = true,
        ) {
            selectedNoteId = CommandItemId(note.id)
            renderFullscreen()
        }
        action(
            title = UiText.Resource("notes.action.delete"),
            icon = Icon.Builtin("Delete"),
            style = CommandListAction.Style.Destructive,
        ) {
            deleteNote(note.id)
            Host.notification.showToast(UiText.Resource("notes.toast.deleted"))
        }
    }

    private fun RayScope.editNoteForm(note: Note) {
        Form(
            navigationTitle = UiText.Resource("notes.edit.title"),
            requireChanges = true,
            unchangedView =
                EmptyViewData(
                    title = UiText.Resource("notes.edit.unchanged.title"),
                    description = UiText.Resource("notes.edit.unchanged.description"),
                    icon = NoteItemIcon,
                    iconColor = NoteMutedIconColor,
                ),
            actionPanelHintMode = ActionPanelHintMode.Hidden,
        ) {
            textField(
                id = "$TITLE_FIELD_ID-${note.id}",
                title = UiText.Resource("notes.field.title"),
                placeholder = UiText.Resource("notes.field.title.placeholder"),
                defaultValue = note.title,
                required = true,
            )
            textArea(
                id = "$BODY_FIELD_ID-${note.id}",
                title = UiText.Resource("notes.field.body"),
                placeholder = UiText.Resource("notes.field.body.placeholder"),
                defaultValue = note.body,
            )
            submit(
                title = UiText.Resource("notes.action.edit"),
                icon = Icon.Builtin("Save"),
                style = FormSubmitStyle.Tonal,
            ) { values ->
                val title = values.text("$TITLE_FIELD_ID-${note.id}").trim()
                if (title.isNotEmpty()) {
                    updateNote(
                        noteId = note.id,
                        title = title,
                        body = values.text("$BODY_FIELD_ID-${note.id}"),
                    )
                    Host.notification.showToast(UiText.Resource("notes.toast.updated"))
                }
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        notes = Host.storage[STORAGE_KEY, NotesState.serializer()] ?: NotesState()
        loaded = true
    }

    private suspend fun createNote(
        title: String,
        body: String,
    ) {
        ensureLoaded()
        val note =
            Note(
                id = nextNoteId(title, body),
                title = title,
                body = body,
            )
        notes = notes.copy(items = listOf(note) + notes.items)
        persist()
        selectedNoteId = CommandItemId(note.id)
        invalidateCache(listOf(CommandItemId(note.id)))
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
        invalidateCache(listOf(CommandItemId(noteId)))
        render()
        renderFullscreen()
    }

    private suspend fun updateNote(
        noteId: String,
        title: String,
        body: String,
    ) {
        ensureLoaded()
        notes =
            notes.copy(
                items =
                    notes.items.map { note ->
                        if (note.id == noteId) {
                            note.copy(title = title, body = body)
                        } else {
                            note
                        }
                    },
            )
        persist()
        selectedNoteId = CommandItemId(noteId)
        invalidateCache(listOf(CommandItemId(noteId)))
        render()
        renderFullscreen()
    }

    private suspend fun persist() {
        Host.storage[STORAGE_KEY, NotesState.serializer()] = notes
    }

    private fun nextNoteId(
        title: String,
        body: String,
    ): String {
        val base = noteIdBase(title)
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
        const val STORAGE_KEY = "notes-state"
        const val TITLE_FIELD_ID = "title"
        const val BODY_FIELD_ID = "body"
        val NotesIcon = Icon.Resource("icons/notes.png")
        val NoteItemIcon = Icon.Builtin("StickyNote2")
        val NoteListIcon = Icon.Builtin("Article")
        val NoteSearchIcon = Icon.Resource("icons/note-search.png")
        val NoteMutedIconColor = Color.OnSurfaceVariant
    }
}
