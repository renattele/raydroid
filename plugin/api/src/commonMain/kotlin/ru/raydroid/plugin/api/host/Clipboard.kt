package ru.raydroid.plugin.api.host

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface Clipboard: ZiplineService {
    suspend fun copy(content: ClipboardContent, options: Options = Options())
    suspend fun clear()
    suspend fun read(options: ReadOptions = ReadOptions()): ClipboardContent

    @Serializable
    data class ClipboardContent(
        val text: String? = null,
        val filePath: String? = null,
        val html: String? = null
    )

    @Serializable
    data class Options(
        val secret: Boolean = false
    )

    @Serializable
    data class ReadOptions(
        val offset: Int = 0
    )
}



