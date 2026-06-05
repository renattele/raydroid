package ru.raydroid.plugin.host.impl.services

import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import kotlin.test.Test
import kotlin.test.assertEquals

class ClipboardServiceBridgeImplTest {
    @Test
    fun `copy and read delegates current clipboard content`() =
        runTest {
            val gateway = FakeClipboardGateway()
            val bridge = ClipboardServiceBridgeImpl(gateway)
            val expected = ClipboardServiceBridge.ClipboardContent(text = "Raydroid")

            bridge.copy(expected, secret = false)

            assertEquals(expected, bridge.read())
        }

    @Test
    fun `clear resets current clipboard content`() =
        runTest {
            val gateway =
                FakeClipboardGateway(
                    content = ClipboardServiceBridge.ClipboardContent(text = "before"),
                )
            val bridge = ClipboardServiceBridgeImpl(gateway)

            bridge.clear()

            assertEquals(ClipboardServiceBridge.ClipboardContent(text = ""), bridge.read())
        }

    @Test
    fun `history offsets beyond current clipboard return empty content`() =
        runTest {
            val gateway =
                FakeClipboardGateway(
                    content = ClipboardServiceBridge.ClipboardContent(text = "ignored"),
                )
            val bridge = ClipboardServiceBridgeImpl(gateway)

            val content = bridge.read(historyOffset = 1)

            assertEquals(ClipboardServiceBridge.ClipboardContent(), content)
        }

    private class FakeClipboardGateway(
        private var content: ClipboardServiceBridge.ClipboardContent = ClipboardServiceBridge.ClipboardContent(),
    ) : ClipboardGateway {
        override fun copy(content: ClipboardServiceBridge.ClipboardContent) {
            this.content = content
        }

        override fun clear() {
            content = ClipboardServiceBridge.ClipboardContent(text = "")
        }

        override fun read(): ClipboardServiceBridge.ClipboardContent = content
    }
}
