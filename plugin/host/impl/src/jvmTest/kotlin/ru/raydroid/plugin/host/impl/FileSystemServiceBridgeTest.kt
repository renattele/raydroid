package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import ru.raydroid.plugin.api.host.exception.PermissionDenied
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import ru.raydroid.plugin.api.manifest.Access
import ru.raydroid.plugin.api.manifest.Command
import ru.raydroid.plugin.api.manifest.FileSystemAccess
import ru.raydroid.plugin.api.manifest.FileSystemAccessPermission
import ru.raydroid.plugin.api.manifest.Manifest
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.host.impl.permission.PermissionFileSystemServiceBridge
import ru.raydroid.plugin.host.impl.services.FileSystemServiceBridgeImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileSystemServiceBridgeTest {
    @Test
    fun `filesystem bridge supports file operations`() = runTest {
        val fs = FakeFileSystem()
        val bridge = FileSystemServiceBridgeImpl(fs)
        fs.createDirectories("/allowed".toPath())

        bridge.createFile("/allowed/file.txt")
        bridge.write("/allowed/file.txt", "hello".encodeToByteArray())
        bridge.createDirectories("/allowed/nested")

        assertEquals(true, bridge.exists("/allowed/file.txt"))
        assertContentEquals("hello".encodeToByteArray(), bridge.read("/allowed/file.txt"))
        assertEquals(FileSystemServiceBridge.RawFileKind.File, bridge.metadata("/allowed/file.txt")?.kind)
        assertEquals(listOf("file.txt", "nested"), bridge.list("/allowed").map { it.name }.sorted())

        bridge.delete("/allowed/file.txt")

        assertEquals(false, bridge.exists("/allowed/file.txt"))
    }

    @Test
    fun `filesystem permission bridge denies missing access`() = runTest {
        val delegate = FileSystemServiceBridgeImpl(FakeFileSystem())
        val bridge = PermissionFileSystemServiceBridge(delegate, testManifest())

        assertFailsWith<PermissionDenied> {
            bridge.exists("/allowed/file.txt")
        }
    }

    @Test
    fun `filesystem permission bridge gates operations and allowed paths`() = runTest {
        val fs = FakeFileSystem()
        fs.createDirectories("/allowed".toPath())
        fs.write("/allowed/file.txt".toPath()) {
            writeUtf8("hello")
            Unit
        }
        val bridge = PermissionFileSystemServiceBridge(
            fileSystemServiceBridge = FileSystemServiceBridgeImpl(fs),
            manifest = testManifest(
                permissions = listOf(
                    FileSystemAccessPermission.Read,
                    FileSystemAccessPermission.Write
                ),
                allowedPaths = listOf("/allowed(/.*)?")
            )
        )

        assertEquals(true, bridge.exists("/allowed/file.txt"))
        bridge.write("/allowed/file.txt", "next".encodeToByteArray())

        assertFailsWith<PermissionDenied> {
            bridge.delete("/allowed/file.txt")
        }
        assertFailsWith<PermissionDenied> {
            bridge.exists("/blocked/file.txt")
        }
    }

    @Test
    fun `filesystem permission bridge allows manage for delete`() = runTest {
        val fs = FakeFileSystem()
        fs.createDirectories("/allowed".toPath())
        fs.write("/allowed/file.txt".toPath()) {
            writeUtf8("hello")
            Unit
        }
        val bridge = PermissionFileSystemServiceBridge(
            fileSystemServiceBridge = FileSystemServiceBridgeImpl(fs),
            manifest = testManifest(
                permissions = listOf(FileSystemAccessPermission.Manage),
                allowedPaths = listOf("/allowed(/.*)?")
            )
        )

        bridge.delete("/allowed/file.txt")

        assertEquals(false, fs.exists("/allowed/file.txt".toPath()))
    }

    @Test
    fun `filesystem watch emits changes`() = runTest {
        val fs = FakeFileSystem()
        fs.createDirectories("/allowed".toPath())
        fs.write("/allowed/file.txt".toPath()) {
            writeUtf8("hello")
            Unit
        }
        val bridge = FileSystemServiceBridgeImpl(fs)
        val deferred = async {
            bridge.watch("/allowed/file.txt").first { event ->
                event.metadata?.size == 4L
            }
        }

        fs.write("/allowed/file.txt".toPath()) {
            writeUtf8("next")
            Unit
        }

        assertEquals(4, deferred.await().metadata?.size)
    }

    private fun testManifest(
        permissions: List<FileSystemAccessPermission> = emptyList(),
        allowedPaths: List<String> = emptyList()
    ): Manifest =
        Manifest(
            name = "ru.test.plugin",
            title = UiText.Plain("Test"),
            description = UiText.Plain("Test manifest"),
            author = UiText.Plain("Codex"),
            version = 1,
            platforms = listOf(Platform.MacOS),
            categories = emptyList(),
            license = "MIT",
            commands = listOf(
                Command(
                    service = "apps",
                    title = UiText.Plain("Apps"),
                    description = UiText.Plain("Apps command"),
                    mode = Command.Mode.View,
                    match = null,
                    arguments = emptyList(),
                    preferences = emptyList(),
                )
            ),
            resources = emptyMap(),
            access = Access(
                filesystem = FileSystemAccess(
                    permissions = permissions,
                    allowedPaths = allowedPaths
                )
            )
        )
}
