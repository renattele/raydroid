package ru.raydroid.plugin.impl.notes.util

internal fun noteIdBase(title: String): String =
    title
        .lowercase()
        .map { char ->
            when {
                char.isLetterOrDigit() -> char
                else -> '-'
            }
        }.joinToString("")
        .trim('-')
        .take(48)
        .ifBlank { "note" }

internal fun stableHash(value: String): String {
    var hash = 5381
    value.forEach { char ->
        hash = ((hash shl 5) + hash) xor char.code
    }
    return hash.toUInt().toString(36)
}
