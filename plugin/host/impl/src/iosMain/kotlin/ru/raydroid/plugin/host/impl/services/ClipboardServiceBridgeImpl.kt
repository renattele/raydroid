package ru.raydroid.plugin.host.impl.services

import platform.UIKit.UIPasteboard
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge

internal class ClipboardServiceBridgeImpl : ClipboardServiceBridge {
    override suspend fun copy(
        content: ClipboardServiceBridge.ClipboardContent,
        secret: Boolean
    ) {
        UIPasteboard.generalPasteboard.string = content.text.orEmpty()
    }

    override suspend fun clear() {
        UIPasteboard.generalPasteboard.string = ""
    }

    override suspend fun read(historyOffset: Int): ClipboardServiceBridge.ClipboardContent =
        ClipboardServiceBridge.ClipboardContent(
            text = UIPasteboard.generalPasteboard.string
        )
}
