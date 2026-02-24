package ru.raydroid.plugin.host

import app.cash.zipline.loader.ZiplineHttpClient
import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.openZip
import okio.use

class ResourceZiplineHttpClient: ZiplineHttpClient() {
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {

    }

}

private val REXT_VIRTUAL_FS_PATH = "plugin.rext".toPath()
private val REXT_UNPACKED_VIRTUAL_FS_PATH = "plugin".toPath()
class RextZiplineHttpClient(val rextFile: ByteString): ZiplineHttpClient() {
    private val fs: FileSystem = FakeFileSystem()
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        if (!fs.exists(REXT_VIRTUAL_FS_PATH)) {
            fs.write(REXT_VIRTUAL_FS_PATH) {
                write(rextFile)
            }
            fs.unpackZip(REXT_VIRTUAL_FS_PATH, REXT_UNPACKED_VIRTUAL_FS_PATH)
        }
        return fs.read(REXT_UNPACKED_VIRTUAL_FS_PATH / url) {
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