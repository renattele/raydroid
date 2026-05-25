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
    private val fileSystem: FileSystem,
    private val platformFileSystemGateway: PlatformFileSystemGateway,
) : FileSystemServiceBridge {
    override suspend fun hasAllFilesAccess(): Boolean = platformFileSystemGateway.hasAllFilesAccess()

    override suspend fun requestAllFilesAccess() {
        platformFileSystemGateway.requestAllFilesAccess()
    }

    override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> = platformFileSystemGateway.listRoots()

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) {
            fileSystem.exists(platformFileSystemGateway.resolveRealPath(path))
        }

    override suspend fun metadata(path: String): FileSystemServiceBridge.RawFileMetadata? =
        withContext(Dispatchers.IO) {
            fileSystem.metadataOrNull(platformFileSystemGateway.resolveRealPath(path))?.toRawMetadata()
        }

    override suspend fun list(path: String): List<FileSystemServiceBridge.RawFileEntry> =
        withContext(Dispatchers.IO) {
            fileSystem.list(platformFileSystemGateway.resolveRealPath(path)).mapNotNull { entry ->
                fileSystem.metadataOrNull(entry)?.toRawMetadata()?.let { metadata ->
                    FileSystemServiceBridge.RawFileEntry(
                        path = platformFileSystemGateway.toVirtualPath(entry),
                        name = entry.name,
                        metadata = metadata,
                    )
                }
            }
        }

    override suspend fun read(path: String): ByteArray =
        withContext(Dispatchers.IO) {
            fileSystem.read(platformFileSystemGateway.resolveRealPath(path)) {
                readByteArray()
            }
        }

    override suspend fun write(
        path: String,
        content: ByteArray,
    ) {
        withContext(Dispatchers.IO) {
            fileSystem.write(platformFileSystemGateway.resolveRealPath(path)) {
                write(content)
                Unit
            }
        }
    }

    override suspend fun createFile(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.write(platformFileSystemGateway.resolveRealPath(path), mustCreate = true) {
                write(ByteArray(0))
                Unit
            }
        }
    }

    override suspend fun createDirectories(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.createDirectories(platformFileSystemGateway.resolveRealPath(path))
        }
    }

    override suspend fun delete(path: String) {
        withContext(Dispatchers.IO) {
            fileSystem.delete(platformFileSystemGateway.resolveRealPath(path))
        }
    }

    override suspend fun watch(path: String): Flow<FileSystemServiceBridge.RawFileChangeEvent> =
        flow {
            val target = platformFileSystemGateway.resolveRealPath(path)
            var previousSnapshot: FileSnapshot? = null
            while (true) {
                val currentSnapshot =
                    withContext(Dispatchers.IO) {
                        fileSystem.snapshot(target)
                    }
                if (currentSnapshot != previousSnapshot) {
                    emit(
                        FileSystemServiceBridge.RawFileChangeEvent(
                            path = path,
                            metadata = currentSnapshot?.metadata,
                        ),
                    )
                    previousSnapshot = currentSnapshot
                }
                delay(WATCH_INTERVAL_MILLIS)
            }
        }

    override suspend fun open(path: String) {
        platformFileSystemGateway.open(path)
    }

    private fun FileSystem.snapshot(path: Path): FileSnapshot? {
        val metadata = metadataOrNull(path)?.toRawMetadata() ?: return null
        val children =
            if (metadata.kind == FileSystemServiceBridge.RawFileKind.Directory) {
                list(path)
                    .mapNotNull { childPath ->
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
        val children: List<Pair<String, String>>,
    )

    private companion object {
        const val WATCH_INTERVAL_MILLIS = 500L
    }
}

private fun FileMetadata.toRawMetadata(): FileSystemServiceBridge.RawFileMetadata =
    FileSystemServiceBridge.RawFileMetadata(
        kind =
            when {
                isRegularFile -> FileSystemServiceBridge.RawFileKind.File
                isDirectory -> FileSystemServiceBridge.RawFileKind.Directory
                symlinkTarget != null -> FileSystemServiceBridge.RawFileKind.Symlink
                else -> FileSystemServiceBridge.RawFileKind.Other
            },
        size = size,
        createdAtEpochMillis = createdAtMillis,
        lastModifiedAtEpochMillis = lastModifiedAtMillis,
    )

private fun FileMetadata.toSnapshotToken(): String =
    listOf(
        isRegularFile.toString(),
        isDirectory.toString(),
        symlinkTarget?.toString().orEmpty(),
        size?.toString().orEmpty(),
        createdAtMillis?.toString().orEmpty(),
        lastModifiedAtMillis?.toString().orEmpty(),
    ).joinToString(separator = "|")
