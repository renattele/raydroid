package ru.raydroid.plugin.host.impl.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import okio.Path
import okio.Path.Companion.toPath
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import java.io.File
import java.net.URLConnection

internal class AndroidFileSystemGateway(
    private val context: Context,
) : PlatformFileSystemGateway {
    override suspend fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    override suspend fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val appUri = Uri.parse("package:${context.packageName}")
        val intent =
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, appUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.getOrElse {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> =
        roots.map { root ->
            FileSystemServiceBridge.RawFileRoot(
                id = root.id,
                name = root.name,
                path = root.virtualPath,
            )
        }

    override suspend fun open(path: String) {
        val file = File(resolveRealPath(path).toString())
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val contentType = URLConnection.guessContentTypeFromName(file.name) ?: "*/*"
        val intent =
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, contentType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val chooser =
            Intent.createChooser(intent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }

    override fun resolveRealPath(path: String): Path {
        val root = roots.firstOrNull { path == it.virtualPath || path.startsWith("${it.virtualPath}/") }
        if (root == null) {
            return path.toPath()
        }
        val relativePath = path.removePrefix(root.virtualPath).trimStart('/')
        return if (relativePath.isEmpty()) {
            root.realPath.toPath()
        } else {
            "${root.realPath}/$relativePath".toPath()
        }
    }

    override fun toVirtualPath(path: Path): String {
        val text = path.toString()
        val root =
            roots.firstOrNull { candidate ->
                text == candidate.realPath || text.startsWith("${candidate.realPath}/")
            } ?: return text
        val relativePath = text.removePrefix(root.realPath).trimStart('/')
        return if (relativePath.isEmpty()) {
            root.virtualPath
        } else {
            "${root.virtualPath}/$relativePath"
        }
    }

    private data class Root(
        val id: String,
        val name: String,
        val realPath: String,
    ) {
        val virtualPath: String = "fs://$id"
    }

    private companion object {
        val roots =
            listOf(
                Root(id = "download", name = "Download", realPath = "/storage/emulated/0/Download"),
                Root(id = "downloads", name = "Downloads", realPath = "/storage/emulated/0/Downloads"),
                Root(id = "documents", name = "Documents", realPath = "/storage/emulated/0/Documents"),
                Root(id = "dcim", name = "DCIM", realPath = "/storage/emulated/0/DCIM"),
                Root(id = "pictures", name = "Pictures", realPath = "/storage/emulated/0/Pictures"),
                Root(id = "movies", name = "Movies", realPath = "/storage/emulated/0/Movies"),
                Root(id = "music", name = "Music", realPath = "/storage/emulated/0/Music"),
                Root(id = "recordings", name = "Recordings", realPath = "/storage/emulated/0/Recordings"),
            )
    }
}
