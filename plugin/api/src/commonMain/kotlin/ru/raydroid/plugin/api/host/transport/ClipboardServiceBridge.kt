package ru.raydroid.plugin.api.host.transport

import app.cash.zipline.ZiplineService
import kotlinx.serialization.Serializable

interface ClipboardServiceBridge : ZiplineService {
    suspend fun copy(
        content: ClipboardContent,
        secret: Boolean = false,
    )

    suspend fun clear()

    suspend fun read(historyOffset: Int = 0): ClipboardContent

    @Serializable
    data class ClipboardContent(
        val text: String? = null,
        val filePath: String? = null,
    )
}
