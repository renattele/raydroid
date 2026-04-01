package ru.raydroid.plugin.host.impl.runtime

import app.cash.zipline.loader.ZiplineHttpClient
import io.ktor.http.Url
import okio.ByteString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use


class RextZiplineHttpClient(private val fs: FileSystem): ZiplineHttpClient() {
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {
        val uri = Url(url)
        // Dropping "/"
        val fullPath = uri.encodedPath.drop(1)
        return fs.read(REXT_UNPACKED_VIRTUAL_FS_PATH / fullPath) {
            readByteString()
        }
    }

}

