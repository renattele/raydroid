package ru.raydroid.plugin.api.host.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.raydroid.plugin.api.host.service.FileChangeEvent
import ru.raydroid.plugin.api.host.service.FileEntry
import ru.raydroid.plugin.api.host.service.FileKind
import ru.raydroid.plugin.api.host.service.FileMetadata
import ru.raydroid.plugin.api.host.service.FileSystemService
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge

internal class FileSystemServiceImpl(
    private val bridge: FileSystemServiceBridge
) : FileSystemService {
    override suspend fun exists(path: String): Boolean = bridge.exists(path)

    override suspend fun metadata(path: String): FileMetadata? =
        bridge.metadata(path)?.toServiceMetadata()

    override suspend fun list(path: String): List<FileEntry> =
        bridge.list(path).map { it.toServiceEntry() }

    override suspend fun read(path: String): ByteArray = bridge.read(path)

    override suspend fun write(path: String, content: ByteArray) {
        bridge.write(path, content)
    }

    override suspend fun createFile(path: String) {
        bridge.createFile(path)
    }

    override suspend fun createDirectories(path: String) {
        bridge.createDirectories(path)
    }

    override suspend fun delete(path: String) {
        bridge.delete(path)
    }

    override suspend fun watch(path: String): Flow<FileChangeEvent> =
        bridge.watch(path).map { it.toServiceEvent() }
}

private fun FileSystemServiceBridge.RawFileMetadata.toServiceMetadata(): FileMetadata =
    FileMetadata(
        kind = kind.toServiceKind(),
        size = size,
        createdAtEpochMillis = createdAtEpochMillis,
        lastModifiedAtEpochMillis = lastModifiedAtEpochMillis
    )

private fun FileSystemServiceBridge.RawFileKind.toServiceKind(): FileKind =
    when (this) {
        FileSystemServiceBridge.RawFileKind.File -> FileKind.File
        FileSystemServiceBridge.RawFileKind.Directory -> FileKind.Directory
        FileSystemServiceBridge.RawFileKind.Symlink -> FileKind.Symlink
        FileSystemServiceBridge.RawFileKind.Other -> FileKind.Other
    }

private fun FileSystemServiceBridge.RawFileEntry.toServiceEntry(): FileEntry =
    FileEntry(
        path = path,
        name = name,
        metadata = metadata.toServiceMetadata()
    )

private fun FileSystemServiceBridge.RawFileChangeEvent.toServiceEvent(): FileChangeEvent =
    FileChangeEvent(
        path = path,
        metadata = metadata?.toServiceMetadata()
    )
