package ru.raydroid.plugin.host.impl.permission

import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.readable
import ru.raydroid.plugin.api.manifest.writable
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.exception.PermissionDenied

internal class PermissionClipboardServiceBridge(
    private val clipboardServiceBridge: ClipboardServiceBridge,
    private val manifest: Manifest
): ClipboardServiceBridge {
    override suspend fun copy(
        content: ClipboardServiceBridge.ClipboardContent,
        secret: Boolean
    ) {
        if (manifest.access.clipboard?.permissions?.readable() == true) {
            clipboardServiceBridge.copy(content, secret)
        } else {
            throw PermissionDenied()
        }
    }

    override suspend fun clear() {
        if (manifest.access.clipboard?.permissions?.writable() == true) {
            clipboardServiceBridge.clear()
        } else {
            throw PermissionDenied()
        }
    }

    override suspend fun read(historyOffset: Int): ClipboardServiceBridge.ClipboardContent {
        if (manifest.access.clipboard?.permissions?.readable() == true) {
            return clipboardServiceBridge.read(historyOffset)
        }
        throw PermissionDenied()
    }
}