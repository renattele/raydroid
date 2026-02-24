package ru.raydroid.plugin.host

import androidx.compose.ui.res.useResource
import app.cash.zipline.loader.ZiplineHttpClient
import okio.ByteString

class ResourceZiplineHttpClient: ZiplineHttpClient() {
    override suspend fun download(
        url: String,
        requestHeaders: List<Pair<String, String>>
    ): ByteString {

    }

}