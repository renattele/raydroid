package ru.raydroid.plugin.impl.notes.util

import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.impl.notes.model.Note

internal fun Note.bodyPreview(): String =
    body.lineSequence()
        .map { line -> line.trim() }
        .firstOrNull { line -> line.isNotEmpty() }
        ?.take(120)
        ?: "Empty note"

internal fun Note.markdown(): String =
    buildString {
        append("# ")
        appendLine(title.ifBlank { "Untitled" })
        appendLine()
        append(body.ifBlank { "_Empty note_" })
    }

internal fun Note.keywords(): List<String> =
    (title.splitWords() + body.splitWords()).distinct().take(16)

internal fun Note.toCommandListItem(icon: Icon): CommandListItem =
    CommandListItem(
        id = CommandItemId(id),
        icon = icon,
        title = UiText.Plain(title),
        description = UiText.Plain(bodyPreview())
    )

private fun String.splitWords(): List<String> =
    split(Regex("\\s+")).map { word -> word.trim() }.filter { word -> word.length > 2 }
