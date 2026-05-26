package ru.raydroid.plugin.impl.files

import kotlinx.coroutines.flow.flow
import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.FileEntry
import ru.raydroid.plugin.api.host.service.FileKind
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListItem
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Detail
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.RayScope

class FilesCommand : CommandService() {
    private var indexedFiles = emptyList<IndexedFile>()
    private var liveFiles = emptyList<IndexedFile>()
    private var selectedFile: IndexedFile? = null
    private var lastOpenedPath: String? = null
    private var lastOpenedAtEpochMillis: Double = 0.0

    override suspend fun cachedItems(
        requestedItems: List<CommandItemId>?,
        chunkSize: Int,
    ) = flow {
        val requestedIds = requestedItems?.map { itemId -> itemId.value }?.toSet()
        if (!Host.filesystem.hasAllFilesAccess()) {
            indexedFiles = if (requestedIds == null) emptyList() else indexedFiles
            return@flow
        }

        val scannedFiles = mutableListOf<IndexedFile>()
        val seenPaths = mutableSetOf<String>()
        val chunk = mutableListOf<CommandListItem>()

        suspend fun emitFile(file: IndexedFile) {
            if (!seenPaths.add(file.path)) return
            if (requestedIds != null && file.id !in requestedIds) return
            scannedFiles += file
            chunk += file.toCommandListItem()
            if (chunk.size >= chunkSize) {
                emit(chunk.toList())
                chunk.clear()
            }
        }

        scanFileRoots(::emitFile)
        if (chunk.isNotEmpty()) emit(chunk.toList())

        indexedFiles =
            if (requestedIds == null) {
                scannedFiles
            } else {
                (indexedFiles.filterNot { it.id in requestedIds } + scannedFiles).distinctBy { it.id }
            }
    }

    override fun RayScope.fullscreen() {
        val file = selectedFile ?: return
        Detail(
            markdown = file.markdown(),
            navigationTitle = UiText.Plain(file.name),
        )
    }

    override fun CommandListScope.content() {
        liveFiles.forEach { file ->
            fileEntry(file)
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        if (target.itemId == CommandItemId.CommandRoot) return
        val file = file(target.itemId.value) ?: return
        action(
            title = UiText.Resource("files.action.open"),
            icon = Icon.Builtin("OpenInNew"),
            primary = true,
        ) {
            openFile(file.path)
        }
        action(
            title = UiText.Resource("files.action.info"),
            icon = Icon.Builtin("Info"),
        ) {
            selectedFile = file
            renderFullscreen()
        }
    }

    override suspend fun execute(action: CommandAction) {
        when (action) {
            is CommandAction.Enter -> {
                if (action.hoveredId == CommandItemId.CommandRoot) {
                    requestFilesAccessAndRefresh()
                    return
                }
                val files =
                    (liveFiles + indexedFiles)
                        .distinctBy { file -> file.id }
                        .ifEmpty { scanFiles().also { indexedFiles = it } }
                val file = files.firstOrNull { file -> file.id == action.hoveredId.value } ?: return
                openFile(file.path)
            }

            is CommandAction.OpenCommand -> {
                requestFilesAccessAndRefresh()
            }

            is CommandAction.CloseCommand -> {
                selectedFile = null
            }

            is CommandAction.Type -> {
                liveFiles = liveFiles(action.query)
                render()
            }

            is CommandAction.Focus -> {
                Unit
            }
        }
    }

    private suspend fun requestFilesAccessAndRefresh() {
        Host.filesystem.requestAllFilesAccess()
        indexedFiles = emptyList()
        liveFiles = emptyList()
        invalidateCache(null)
        render()
    }

    private suspend fun openFile(path: String) {
        val now = nowEpochMillis().toDouble()
        if (lastOpenedPath == path && now - lastOpenedAtEpochMillis < OPEN_DEBOUNCE_MILLIS) {
            return
        }
        lastOpenedPath = path
        lastOpenedAtEpochMillis = now
        Host.filesystem.open(path)
    }

    private suspend fun scanFiles(): List<IndexedFile> {
        if (!Host.filesystem.hasAllFilesAccess()) return emptyList()
        val files = mutableListOf<IndexedFile>()
        val seenPaths = mutableSetOf<String>()
        scanFileRoots { file ->
            if (seenPaths.add(file.path)) files += file
        }
        return files
    }

    private suspend fun liveFiles(query: String): List<IndexedFile> {
        if (!Host.filesystem.hasAllFilesAccess()) return emptyList()
        val normalizedQuery = query.trim().lowercase()
        return if (normalizedQuery.isBlank()) {
            recentFiles()
        } else {
            matchingFiles(normalizedQuery)
        }
    }

    private suspend fun matchingFiles(query: String): List<IndexedFile> {
        val terms = query.split(WhitespaceRegex).filter { term -> term.isNotBlank() }
        if (terms.isEmpty()) return recentFiles()
        val files = mutableListOf<IndexedFile>()
        val seenPaths = mutableSetOf<String>()
        scanFileRoots { file ->
            if (files.size >= MAX_LIVE_SEARCH_FILES) return@scanFileRoots
            if (!seenPaths.add(file.path)) return@scanFileRoots
            if (file.matches(terms)) files += file
        }
        return files
    }

    private suspend fun recentFiles(): List<IndexedFile> {
        val minModifiedAt = nowEpochMillis() - RECENT_FILES_WINDOW_MILLIS
        val files = mutableListOf<IndexedFile>()
        val seenPaths = mutableSetOf<String>()
        scanFileRoots { file ->
            if (!seenPaths.add(file.path)) return@scanFileRoots
            val lastModifiedAt = file.lastModifiedAtEpochMillis ?: return@scanFileRoots
            if (lastModifiedAt >= minModifiedAt) files += file
        }
        return files
            .sortedByDescending { file -> file.lastModifiedAtEpochMillis ?: 0L }
            .take(MAX_RECENT_FILES)
    }

    private suspend fun scanFileRoots(onFile: suspend (IndexedFile) -> Unit) {
        var indexedCount = 0

        suspend fun emit(file: IndexedFile) {
            if (indexedCount >= MAX_INDEXED_FILES) return
            indexedCount++
            onFile(file)
        }
        Host.filesystem.listRoots().forEach { root ->
            scanDirectory(root.path, depth = 0, onFile = ::emit)
        }
    }

    private suspend fun scanDirectory(
        path: String,
        depth: Int,
        onFile: suspend (IndexedFile) -> Unit,
    ) {
        if (depth > MAX_DEPTH) return
        val entries = runCatching { Host.filesystem.list(path) }.getOrElse { return }
        entries.forEach { entry ->
            if (entry.name.isHiddenFileName()) return@forEach
            when (entry.metadata.kind) {
                FileKind.File -> onFile(entry.toIndexedFile())

                FileKind.Directory -> scanDirectory(entry.path, depth + 1, onFile)

                FileKind.Symlink,
                FileKind.Other,
                -> Unit
            }
        }
    }

    private fun FileEntry.toIndexedFile(): IndexedFile =
        IndexedFile(
            id = path.toItemId(),
            path = path,
            name = name,
            size = metadata.size,
            lastModifiedAtEpochMillis = metadata.lastModifiedAtEpochMillis,
        )

    private fun IndexedFile.toCommandListItem(): CommandListItem =
        CommandListItem(
            id = CommandItemId(id),
            title = UiText.Plain(name),
            description = UiText.Plain(parentPath()),
            icon = icon(),
            iconColor = Color.OnSurfaceVariant,
        )

    private fun CommandListScope.fileEntry(file: IndexedFile) {
        val item = file.toCommandListItem()
        entry(
            id = item.id,
            title = item.title,
            description = item.description,
            icon = item.icon,
            iconColor = item.iconColor,
            trailingText = item.trailingText,
            quickAction = item.quickAction,
        )
    }

    private fun IndexedFile.markdown(): String =
        """
        # $name

        `${path.escapeMarkdown()}`

        Size: ${size?.formatBytes() ?: "Unknown"}
        """.trimIndent()

    private fun file(id: String): IndexedFile? =
        (liveFiles + indexedFiles)
            .distinctBy { file -> file.id }
            .firstOrNull { file -> file.id == id }

    private data class IndexedFile(
        val id: String,
        val path: String,
        val name: String,
        val size: Long?,
        val lastModifiedAtEpochMillis: Long?,
    )

    private companion object {
        const val MAX_DEPTH = 6
        const val MAX_INDEXED_FILES = 5_000
        const val MAX_LIVE_SEARCH_FILES = 50
        const val MAX_RECENT_FILES = 25
        const val RECENT_FILES_WINDOW_MILLIS = 7L * 24L * 60L * 60L * 1_000L
        const val OPEN_DEBOUNCE_MILLIS = 750.0

        val WhitespaceRegex = Regex("\\s+")

        fun String.toItemId(): String =
            "file:" + encodeToByteArray().joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }

        fun nowEpochMillis(): Long =
            kotlin.js
                .Date()
                .getTime()
                .toLong()

        fun IndexedFile.parentPath(): String = path.substringBeforeLast("/", missingDelimiterValue = path)

        fun IndexedFile.extension(): String = name.substringAfterLast(".", missingDelimiterValue = "").lowercase()

        fun String.isHiddenFileName(): Boolean = startsWith(".")

        fun IndexedFile.matches(terms: List<String>): Boolean {
            val searchableText =
                listOf(name, extension(), path)
                    .joinToString(separator = " ")
                    .lowercase()
            return terms.all { term -> searchableText.contains(term) }
        }

        fun IndexedFile.icon(): Icon =
            when (name.substringAfterLast(".", "").lowercase()) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> Icon.Builtin("Image")
                "mp3", "wav", "ogg", "flac", "m4a" -> Icon.Builtin("AudioFile")
                "mp4", "mkv", "webm", "mov", "avi" -> Icon.Builtin("VideoFile")
                "pdf" -> Icon.Builtin("PictureAsPdf")
                "zip", "rar", "7z", "tar", "gz" -> Icon.Builtin("FolderZip")
                "kt", "java", "js", "ts", "json", "xml", "html", "css", "md" -> Icon.Builtin("Code")
                else -> Icon.Builtin("Description")
            }

        fun Long.formatBytes(): String {
            if (this < 1024) return "$this B"
            val units = listOf("KB", "MB", "GB", "TB")
            var value = this.toDouble() / 1024.0
            var index = 0
            while (value >= 1024.0 && index < units.lastIndex) {
                value /= 1024.0
                index++
            }
            val text =
                if (value >= 10) {
                    value.toInt().toString()
                } else {
                    (value * 10).toInt().let {
                        "${it / 10}.${it % 10}"
                    }
                }
            return "$text ${units[index]}"
        }

        fun String.escapeMarkdown(): String = replace("`", "\\`")
    }
}
