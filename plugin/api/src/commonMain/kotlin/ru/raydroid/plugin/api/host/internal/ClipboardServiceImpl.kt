package ru.raydroid.plugin.api.host.internal

import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.service.ClipboardService

internal class ClipboardServiceImpl(
    private val bridge: ClipboardServiceBridge
): ClipboardService {
    override suspend fun copy(
        content: ClipboardService.ClipboardContent,
        secret: Boolean
    ) {
        bridge.copy(content.toBridgeContent(), secret)
    }

    override suspend fun clear() {
        bridge.clear()
    }

    override suspend fun read(historyOffset: Int): ClipboardService.ClipboardContent {
        return bridge.read(historyOffset).toServiceContent()
    }

    private fun ClipboardServiceBridge.ClipboardContent.toServiceContent() = ClipboardService.ClipboardContent(
        text = text,
        filePath = filePath
    )

    private fun ClipboardService.ClipboardContent.toBridgeContent() = ClipboardServiceBridge.ClipboardContent(
        text = text,
        filePath = filePath
    )
}