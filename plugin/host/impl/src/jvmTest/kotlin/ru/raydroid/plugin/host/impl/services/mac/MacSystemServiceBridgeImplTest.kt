package ru.raydroid.plugin.host.impl.services.mac

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MacSystemServiceBridgeImplTest {
    @Test
    fun `open keeps web urls intact`() =
        runTest {
            val commands = mutableListOf<List<String>>()
            val bridge = MacSystemServiceBridgeImpl(commandExecutor = { command -> commands += command })

            bridge.open("https://raydroid.ru")

            assertEquals(
                listOf("open", "https://raydroid.ru"),
                commands.single(),
            )
        }

    @Test
    fun `open converts file urls into absolute paths`() =
        runTest {
            val commands = mutableListOf<List<String>>()
            val bridge = MacSystemServiceBridgeImpl(commandExecutor = { command -> commands += command })

            bridge.open("file:///Users/tester/Desktop/file.txt")

            assertEquals(
                listOf("open", "/Users/tester/Desktop/file.txt"),
                commands.single(),
            )
        }

    @Test
    fun `open keeps absolute file paths as direct open targets`() =
        runTest {
            val commands = mutableListOf<List<String>>()
            val bridge = MacSystemServiceBridgeImpl(commandExecutor = { command -> commands += command })

            bridge.open("/Users/tester/Downloads/file.txt")

            assertEquals(
                listOf("open", "/Users/tester/Downloads/file.txt"),
                commands.single(),
            )
        }
}
