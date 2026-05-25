package ru.raydroid.plugin.host.impl.services

import okio.Path
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge

internal interface PlatformFileSystemGateway {
    suspend fun hasAllFilesAccess(): Boolean

    suspend fun requestAllFilesAccess()

    suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot>

    suspend fun open(path: String)

    fun resolveRealPath(path: String): Path

    fun toVirtualPath(path: Path): String
}

internal class PassthroughPlatformFileSystemGateway : PlatformFileSystemGateway {
    override suspend fun hasAllFilesAccess(): Boolean = false

    override suspend fun requestAllFilesAccess() = Unit

    override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> = emptyList()

    override suspend fun open(path: String) = Unit

    override fun resolveRealPath(path: String): Path = path.toPath()

    override fun toVirtualPath(path: Path): String = path.toString()
}
