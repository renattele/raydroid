package ru.raydroid.plugin.host.impl.services


import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge


internal class ClipboardServiceBridgeImpl: ClipboardServiceBridge {
    override suspend fun copy(
        content: ClipboardServiceBridge.ClipboardContent,
        secret: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }

    override suspend fun read(historyOffset: Int): ClipboardServiceBridge.ClipboardContent {
        TODO("Not yet implemented")
    }

}