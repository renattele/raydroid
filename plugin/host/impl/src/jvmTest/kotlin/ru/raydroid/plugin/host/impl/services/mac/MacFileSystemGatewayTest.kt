package ru.raydroid.plugin.host.impl.services.mac

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacFileSystemGatewayTest {
    @Test
    fun `list roots exposes stable mac locations that exist`() =
        runTest {
            val fileSystem = FakeFileSystem()
            fileSystem.createDirectories("/Users/tester".toPath())
            fileSystem.createDirectories("/Users/tester/Desktop".toPath())
            fileSystem.createDirectories("/Users/tester/Documents".toPath())
            fileSystem.createDirectories("/Users/tester/Downloads".toPath())
            fileSystem.createDirectories("/Applications".toPath())
            val gateway =
                MacFileSystemGateway(
                    fileSystem = fileSystem,
                    userHomePath = "/Users/tester",
                )

            val roots = gateway.listRoots()

            assertEquals(
                listOf("Home", "Desktop", "Documents", "Downloads", "Applications"),
                roots.map { root -> root.name },
            )
            assertEquals("/Users/tester", roots.first().path)
            assertTrue(gateway.hasAllFilesAccess())
        }

    @Test
    fun `open normalizes file urls into open command arguments`() =
        runTest {
            val commands = mutableListOf<List<String>>()
            val gateway =
                MacFileSystemGateway(
                    userHomePath = "/Users/tester",
                    commandExecutor = { command -> commands += command },
                )

            gateway.open("file:///Users/tester/Documents/report.txt")

            assertEquals(
                listOf("open", "/Users/tester/Documents/report.txt"),
                commands.single(),
            )
        }
}
