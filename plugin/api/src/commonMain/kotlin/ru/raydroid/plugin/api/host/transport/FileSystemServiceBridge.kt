package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface FileSystemServiceBridge : ZiplineService {
    suspend fun hasAllFilesAccess(): Boolean

    suspend fun requestAllFilesAccess()

    suspend fun listRoots(): List<RawFileRoot>

    suspend fun exists(path: String): Boolean

    suspend fun metadata(path: String): RawFileMetadata?

    suspend fun list(path: String): List<RawFileEntry>

    suspend fun read(path: String): ByteArray

    suspend fun write(
        path: String,
        content: ByteArray,
    )

    suspend fun createFile(path: String)

    suspend fun createDirectories(path: String)

    suspend fun delete(path: String)

    suspend fun watch(path: String): Flow<RawFileChangeEvent>

    suspend fun open(path: String)

    @Serializable
    data class RawFileRoot(
        val id: String,
        val name: String,
        val path: String,
    )

    @Serializable
    data class RawFileMetadata(
        val kind: RawFileKind,
        val size: Long?,
        val createdAtEpochMillis: Long?,
        val lastModifiedAtEpochMillis: Long?,
    )

    @Serializable
    data class RawFileEntry(
        val path: String,
        val name: String,
        val metadata: RawFileMetadata,
    )

    @Serializable
    data class RawFileChangeEvent(
        val path: String,
        val metadata: RawFileMetadata?,
    )

    @Serializable
    enum class RawFileKind {
        File,
        Directory,
        Symlink,
        Other,
    }
}
