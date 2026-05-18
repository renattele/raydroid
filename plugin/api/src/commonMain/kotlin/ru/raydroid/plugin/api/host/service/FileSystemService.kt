package ru.raydroid.plugin.api.host.service

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface FileSystemService {
    suspend fun hasAllFilesAccess(): Boolean
    suspend fun requestAllFilesAccess()
    suspend fun exists(path: String): Boolean
    suspend fun metadata(path: String): FileMetadata?
    suspend fun list(path: String): List<FileEntry>
    suspend fun read(path: String): ByteArray
    suspend fun write(path: String, content: ByteArray)
    suspend fun createFile(path: String)
    suspend fun createDirectories(path: String)
    suspend fun delete(path: String)
    suspend fun watch(path: String): Flow<FileChangeEvent>
}

@Serializable
data class FileMetadata(
    val kind: FileKind,
    val size: Long?,
    val createdAtEpochMillis: Long?,
    val lastModifiedAtEpochMillis: Long?
)

@Serializable
data class FileEntry(
    val path: String,
    val name: String,
    val metadata: FileMetadata
)

@Serializable
data class FileChangeEvent(
    val path: String,
    val metadata: FileMetadata?
)

@Serializable
enum class FileKind {
    File,
    Directory,
    Symlink,
    Other
}
