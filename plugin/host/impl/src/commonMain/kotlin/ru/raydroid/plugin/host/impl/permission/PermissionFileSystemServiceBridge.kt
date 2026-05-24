package ru.raydroid.plugin.host.impl.permission

import kotlinx.coroutines.flow.Flow
import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import ru.raydroid.plugin.api.manifest.FileSystemAccessPermission
import ru.raydroid.plugin.api.manifest.Manifest

internal class PermissionFileSystemServiceBridge(
    private val fileSystemServiceBridge: FileSystemServiceBridge,
    private val manifest: Manifest,
) : FileSystemServiceBridge {
    override suspend fun hasAllFilesAccess(): Boolean {
        requireFileSystemReadAccess()
        return fileSystemServiceBridge.hasAllFilesAccess()
    }

    override suspend fun requestAllFilesAccess() {
        requireFileSystemReadAccess()
        fileSystemServiceBridge.requestAllFilesAccess()
    }

    override suspend fun exists(path: String): Boolean {
        requireRead(path)
        return fileSystemServiceBridge.exists(path)
    }

    override suspend fun metadata(path: String): FileSystemServiceBridge.RawFileMetadata? {
        requireRead(path)
        return fileSystemServiceBridge.metadata(path)
    }

    override suspend fun list(path: String): List<FileSystemServiceBridge.RawFileEntry> {
        requireRead(path)
        return fileSystemServiceBridge.list(path)
    }

    override suspend fun read(path: String): ByteArray {
        requireRead(path)
        return fileSystemServiceBridge.read(path)
    }

    override suspend fun write(
        path: String,
        content: ByteArray,
    ) {
        requireWrite(path)
        fileSystemServiceBridge.write(path, content)
    }

    override suspend fun createFile(path: String) {
        requireWrite(path)
        fileSystemServiceBridge.createFile(path)
    }

    override suspend fun createDirectories(path: String) {
        requireWrite(path)
        fileSystemServiceBridge.createDirectories(path)
    }

    override suspend fun delete(path: String) {
        requireManage(path)
        fileSystemServiceBridge.delete(path)
    }

    override suspend fun watch(path: String): Flow<FileSystemServiceBridge.RawFileChangeEvent> {
        requireWatch(path)
        return fileSystemServiceBridge.watch(path)
    }

    private fun requireRead(path: String) {
        requireAccess(path, FileSystemAccessPermission.Read)
    }

    private fun requireWrite(path: String) {
        requireAccess(path, FileSystemAccessPermission.Write)
    }

    private fun requireManage(path: String) {
        requireAccess(path, FileSystemAccessPermission.Manage)
    }

    private fun requireWatch(path: String) {
        requireAccess(path, FileSystemAccessPermission.Watch)
    }

    private fun requireFileSystemReadAccess() {
        val permissions = manifest.access.filesystem?.permissions ?: throw PermissionDenied()
        if (FileSystemAccessPermission.Read !in permissions && FileSystemAccessPermission.Manage !in permissions) {
            throw PermissionDenied()
        }
    }

    private fun requireAccess(
        path: String,
        permission: FileSystemAccessPermission,
    ) {
        val access = manifest.access.filesystem ?: throw PermissionDenied()
        val permissions = access.permissions ?: throw PermissionDenied()
        if (permission !in permissions) {
            throw PermissionDenied()
        }
        val allowedPaths = access.allowedPaths.orEmpty()
        if (allowedPaths.isEmpty() || allowedPaths.none { it.toRegex().matches(path) }) {
            throw PermissionDenied()
        }
    }
}
