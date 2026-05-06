package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okio.FileMetadata
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge

internal class FileSystemServiceBridgeImpl(
    private val fileSystem: FileSystem
) : FileSystemServiceBridge {
    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        fileSystem.exists(path.toPath())
    }

    override suspend fun metadata(path: String): FileSystemServiceBridge.RawFileMetadata? =
        withContext(Dispatchers.IO) {
            fileSystem.metadataOrNull(path.toPath())?.toRawMetadata()
        }

    override suspend fun list(path: String): List<FileSystemServiceBridge.RawFileEntry> =
        withContext(Dispatchers.IO) {
            fileSystem.list(path.toPath()).mapNotNull { entry ->
                fileSystem.metadataOrNull(entry)?.toRawMetadata()?.let { metadata ->
                    FileSystemServiceBridge.RawFileEntry(
                        path = entry.toString(),
                        name = entry.name,
                        metadata = metadata
                    )
                }
            }
        }

    override suspend fun read(path: String): ByteArray = withContext(Dispatchers.IO) {
        fileSystem.read(path.toPath()) {
            readByteArray()
        }
    }

    override suspend fun write(path: String, content: ByteArray) {
        withContext(Dispatchers.IO) {
            fileSystem.write(path.toPath()) {
                write(content)
                Unit
            }
        }
    }

    override suspend fun createFile(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.write(path.toPath(), mustCreate = true) {
                write(ByteArray(0))
                Unit
            }
        }
    }

    override suspend fun createDirectories(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.createDirectories(path.toPath())
        }
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.delete(path.toPath())
        }
    }

    override suspend fun watch(path: String): Flow<FileSystemServiceBridge.RawFileChangeEvent> =
        flow {
            val target = path.toPath()
            var previousSnapshot: FileSnapshot? = null
            while (true) {
                val currentSnapshot = withContext(Dispatchers.IO) {
                    fileSystem.snapshot(target)
                }
                if (currentSnapshot != previousSnapshot) {
                    emit(
                        FileSystemServiceBridge.RawFileChangeEvent(
                            path = path,
                            metadata = currentSnapshot?.metadata
                        )
                    )
                    previousSnapshot = currentSnapshot
                }
                delay(WATCH_INTERVAL_MILLIS)
            }
        }

    private fun FileSystem.snapshot(path: Path): FileSnapshot? {
        val metadata = metadataOrNull(path)?.toRawMetadata() ?: return null
        val children = if (metadata.kind == FileSystemServiceBridge.RawFileKind.Directory) {
            list(path).mapNotNull { childPath ->
                metadataOrNull(childPath)?.let { childMetadata ->
                    childPath.toString() to childMetadata.toSnapshotToken()
                }
            }.sortedBy { it.first }
        } else {
            emptyList()
        }
        return FileSnapshot(metadata, children)
    }

    private data class FileSnapshot(
        val metadata: FileSystemServiceBridge.RawFileMetadata,
        val children: List<Pair<String, String>>
    )

    private companion object {
        const val WATCH_INTERVAL_MILLIS = 500L
    }
}

private fun FileMetadata.toRawMetadata(): FileSystemServiceBridge.RawFileMetadata =
    FileSystemServiceBridge.RawFileMetadata(
        kind = when {
            isRegularFile -> FileSystemServiceBridge.RawFileKind.File
            isDirectory -> FileSystemServiceBridge.RawFileKind.Directory
            symlinkTarget != null -> FileSystemServiceBridge.RawFileKind.Symlink
            else -> FileSystemServiceBridge.RawFileKind.Other
        },
        size = size,
        createdAtEpochMillis = createdAtMillis,
        lastModifiedAtEpochMillis = lastModifiedAtMillis
    )

private fun FileMetadata.toSnapshotToken(): String =
    listOf(
        isRegularFile.toString(),
        isDirectory.toString(),
        symlinkTarget?.toString().orEmpty(),
        size?.toString().orEmpty(),
        createdAtMillis?.toString().orEmpty(),
        lastModifiedAtMillis?.toString().orEmpty()
    ).joinToString(separator = "|")
