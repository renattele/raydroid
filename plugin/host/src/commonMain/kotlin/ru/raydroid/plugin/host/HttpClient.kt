package ru.raydroid.plugin.host

import app.cash.zipline.loader.ZiplineHttpClient
import io.ktor.http.Url
import io.ktor.http.fullPath
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.openZip
import okio.use
import raydroid.plugin.host.generated.resources.Res


class ResourceZiplineHttpClient(private val pluginPath: String): ZiplineHttpClient() {
    private var parentClient: ZiplineHttpClient? = null
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        if (parentClient == null) {
            val bytes = Res.readBytes("files/$pluginPath")
            parentClient = RextZiplineHttpClient(bytes.toByteString())
        }
        return parentClient!!.download(url, requestHeaders)
    }
}

private val REXT_VIRTUAL_FS_PATH = "/plugin.rext".toPath()
private val REXT_UNPACKED_VIRTUAL_FS_PATH = "/plugin".toPath()
class RextZiplineHttpClient(val rextFileContent: ByteString): ZiplineHttpClient() {
    private val fs: FileSystem = FakeFileSystem()
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        val uri = Url(url)
        // Dropping "/"
        val fullPath = uri.encodedPath.drop(1)
        if (!fs.exists(REXT_VIRTUAL_FS_PATH)) {
            fs.write(REXT_VIRTUAL_FS_PATH) {
                write(rextFileContent)
            }
            fs.unpackZip(REXT_VIRTUAL_FS_PATH, REXT_UNPACKED_VIRTUAL_FS_PATH)
            fs.listRecursively("/".toPath()).forEach {
                println(it)
            }
        }

        return fs.read(REXT_UNPACKED_VIRTUAL_FS_PATH / fullPath) {
            readByteString()
        }
    }

}

fun FileSystem.unpackZip(zipFile: Path, destDir: Path) {
    fun Path.createParentDirectories() {
        this.parent?.let { parent ->
            createDirectories(parent)
        }
    }

    val zipFileSystem = openZip(zipFile)
    val paths = zipFileSystem.listRecursively("/".toPath())
        .filter { zipFileSystem.metadata(it).isRegularFile }
        .toList()

    paths.forEach { zipFilePath ->
        zipFileSystem.source(zipFilePath).buffer().use { source ->
            val relativeFilePath = zipFilePath.toString().trimStart('/')
            val fileToWrite = destDir.resolve(relativeFilePath)
            fileToWrite.createParentDirectories()
            sink(fileToWrite).buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }
}