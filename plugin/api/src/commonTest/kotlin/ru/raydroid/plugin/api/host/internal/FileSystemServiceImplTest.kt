package ru.raydroid.plugin.api.host.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import ru.raydroid.plugin.api.host.service.FileKind
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FileSystemServiceImplTest {
    @Test
    fun `filesystem service maps bridge models`() =
        runTest {
            val bridge = FakeFileSystemServiceBridge()
            val service = FileSystemServiceImpl(bridge)

            assertEquals(true, service.hasAllFilesAccess())
            service.requestAllFilesAccess()
            assertEquals(listOf("fs://documents"), service.listRoots().map { it.path })
            assertEquals(true, service.exists("/tmp/file.txt"))
            assertEquals(FileKind.File, service.metadata("/tmp/file.txt")?.kind)
            assertEquals("file.txt", service.list("/tmp").single().name)
            assertContentEquals("hello".encodeToByteArray(), service.read("/tmp/file.txt"))

            service.write("/tmp/file.txt", "next".encodeToByteArray())
            service.createFile("/tmp/created.txt")
            service.createDirectories("/tmp/dir")
            service.delete("/tmp/old.txt")
            val event = service.watch("/tmp/file.txt").single()
            service.open("/tmp/file.txt")

            assertEquals("/tmp/file.txt", event.path)
            assertEquals(FileKind.File, event.metadata?.kind)
            assertEquals(
                listOf(
                    "hasAllFilesAccess",
                    "requestAllFilesAccess",
                    "listRoots",
                    "exists:/tmp/file.txt",
                    "metadata:/tmp/file.txt",
                    "list:/tmp",
                    "read:/tmp/file.txt",
                    "write:/tmp/file.txt:next",
                    "createFile:/tmp/created.txt",
                    "createDirectories:/tmp/dir",
                    "delete:/tmp/old.txt",
                    "watch:/tmp/file.txt",
                    "open:/tmp/file.txt",
                ),
                bridge.calls,
            )
        }

    private class FakeFileSystemServiceBridge : FileSystemServiceBridge {
        val calls = mutableListOf<String>()
        private val metadata =
            FileSystemServiceBridge.RawFileMetadata(
                kind = FileSystemServiceBridge.RawFileKind.File,
                size = 5,
                createdAtEpochMillis = 1,
                lastModifiedAtEpochMillis = 2,
            )

        override suspend fun exists(path: String): Boolean {
            calls += "exists:$path"
            return true
        }

        override suspend fun hasAllFilesAccess(): Boolean {
            calls += "hasAllFilesAccess"
            return true
        }

        override suspend fun requestAllFilesAccess() {
            calls += "requestAllFilesAccess"
        }

        override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> {
            calls += "listRoots"
            return listOf(
                FileSystemServiceBridge.RawFileRoot(
                    id = "documents",
                    name = "Documents",
                    path = "fs://documents",
                ),
            )
        }

        override suspend fun metadata(path: String): FileSystemServiceBridge.RawFileMetadata {
            calls += "metadata:$path"
            return metadata
        }

        override suspend fun list(path: String): List<FileSystemServiceBridge.RawFileEntry> {
            calls += "list:$path"
            return listOf(
                FileSystemServiceBridge.RawFileEntry(
                    path = "$path/file.txt",
                    name = "file.txt",
                    metadata = metadata,
                ),
            )
        }

        override suspend fun read(path: String): ByteArray {
            calls += "read:$path"
            return "hello".encodeToByteArray()
        }

        override suspend fun write(
            path: String,
            content: ByteArray,
        ) {
            calls += "write:$path:${content.decodeToString()}"
        }

        override suspend fun createFile(path: String) {
            calls += "createFile:$path"
        }

        override suspend fun createDirectories(path: String) {
            calls += "createDirectories:$path"
        }

        override suspend fun delete(path: String) {
            calls += "delete:$path"
        }

        override suspend fun watch(path: String): Flow<FileSystemServiceBridge.RawFileChangeEvent> {
            calls += "watch:$path"
            return flowOf(
                FileSystemServiceBridge.RawFileChangeEvent(
                    path = path,
                    metadata = metadata,
                ),
            )
        }

        override suspend fun open(path: String) {
            calls += "open:$path"
        }
    }
}
