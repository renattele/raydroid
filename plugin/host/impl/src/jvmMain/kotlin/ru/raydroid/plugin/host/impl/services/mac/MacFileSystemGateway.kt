package ru.raydroid.plugin.host.impl.services.mac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import ru.raydroid.plugin.host.impl.services.PlatformFileSystemGateway
import java.io.File
import java.net.URI

internal class MacFileSystemGateway(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    userHomePath: String = System.getProperty("user.home"),
    private val commandExecutor: (List<String>) -> Unit = ::runCommand,
) : PlatformFileSystemGateway {
    private val userHome = userHomePath.toPath()
    private val roots =
        listOf(
            RootDescriptor("home", "Home", userHome),
            RootDescriptor("desktop", "Desktop", userHome / "Desktop"),
            RootDescriptor("documents", "Documents", userHome / "Documents"),
            RootDescriptor("downloads", "Downloads", userHome / "Downloads"),
            RootDescriptor("applications", "Applications", "/Applications".toPath()),
            RootDescriptor("system-applications", "System Applications", "/System/Applications".toPath()),
        )

    override suspend fun hasAllFilesAccess(): Boolean = true

    override suspend fun requestAllFilesAccess() = Unit

    override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> =
        roots
            .filter { root -> fileSystem.exists(root.path) }
            .map { root ->
                FileSystemServiceBridge.RawFileRoot(
                    id = root.id,
                    name = root.name,
                    path = root.path.toString(),
                )
            }

    override suspend fun open(path: String) {
        withContext(Dispatchers.IO) {
            commandExecutor(
                listOf(
                    "open",
                    normalizeOpenTarget(path),
                ),
            )
        }
    }

    override fun resolveRealPath(path: String): Path =
        when {
            path.startsWith("file://") -> {
                runCatching { File(URI(path)).absolutePath.toPath() }
                    .getOrElse { path.removePrefix("file://").toPath() }
            }

            else -> {
                path.toPath()
            }
        }

    override fun toVirtualPath(path: Path): String = path.toString()

    private data class RootDescriptor(
        val id: String,
        val name: String,
        val path: Path,
    )
}
