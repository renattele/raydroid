package ru.raydroid.plugin.host.impl.services

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.QuickLook.QLPreviewController
import platform.QuickLook.QLPreviewControllerDataSourceProtocol
import platform.QuickLook.QLPreviewControllerDelegateProtocol
import platform.QuickLook.QLPreviewItemProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UINavigationController
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.darwin.NSObject
import platform.posix.memcpy
import ru.raydroid.plugin.api.host.transport.FileSystemServiceBridge
import kotlin.coroutines.resume

internal class IOSFileSystemGateway(
    private val basePath: Path,
) : PlatformFileSystemGateway {
    private val storageDir = basePath / "ios-filesystem"
    private val metadataPath = storageDir / "roots.json"
    private val bookmarkDir = storageDir / "bookmarks"
    private var cachedRoots: List<IOSResolvedRoot>? = null

    override suspend fun hasAllFilesAccess(): Boolean = listRoots().isNotEmpty()

    override suspend fun requestAllFilesAccess() {
        val pickedUrls = pickDirectories()
        if (pickedUrls.isEmpty()) return
        val existingRoots = resolveRoots()
        val rootsByPath = existingRoots.associateBy { it.url.path.orEmpty() }
        val documentsById = loadStoredRoots().associateBy { it.id }.toMutableMap()

        FileSystem.SYSTEM.createDirectories(bookmarkDir)

        pickedUrls.forEach { url ->
            val accessGranted = url.startAccessingSecurityScopedResource()
            if (!accessGranted) return@forEach
            try {
                val bookmarkData = url.bookmarkData() ?: return@forEach
                val path = url.path.orEmpty()
                val existingRoot = rootsByPath[path]
                val rootId = existingRoot?.id ?: NSUUID().UUIDString()
                FileSystem.SYSTEM.write(bookmarkDir / "$rootId.bookmark") {
                    write(bookmarkData.toByteArray())
                }
                documentsById[rootId] =
                    IOSStoredRoot(
                        id = rootId,
                        name = url.lastPathComponent.orEmpty().ifBlank { path.substringAfterLast('/') },
                        bookmarkFileName = "$rootId.bookmark",
                    )
            } finally {
                url.stopAccessingSecurityScopedResource()
            }
        }

        saveStoredRoots(documentsById.values.sortedBy { it.name })
        clearResolvedRoots()
        resolveRoots()
    }

    override suspend fun listRoots(): List<FileSystemServiceBridge.RawFileRoot> =
        resolveRoots().map { root ->
            FileSystemServiceBridge.RawFileRoot(
                id = root.id,
                name = root.name,
                path = root.virtualPath,
            )
        }

    override suspend fun open(path: String) {
        val realPath = resolveRealPath(path).toString()
        withContext(Dispatchers.Main) {
            val url = NSURL.fileURLWithPath(realPath)
            val presenter = topViewController()
            if (presenter == null) {
                UIApplication.sharedApplication.openURL(url)
                return@withContext
            }
            if (presenter is QLPreviewController && FilePreviewPresentationState.activePath == realPath) {
                return@withContext
            }
            if (FilePreviewPresentationState.activePath == realPath) {
                return@withContext
            }
            val previewController = QLPreviewController()
            val coordinator = FilePreviewCoordinator(url)
            previewController.dataSource = coordinator
            previewController.delegate = coordinator
            PresentationRetainer.retain(coordinator)
            FilePreviewPresentationState.activePath = realPath
            val navigationController = UINavigationController(rootViewController = previewController)
            presenter.presentViewController(navigationController, animated = true, completion = null)
        }
    }

    override fun resolveRealPath(path: String): Path {
        val root = resolveRoots().firstOrNull { candidate -> path == candidate.virtualPath || path.startsWith("${candidate.virtualPath}/") }
        if (root == null) {
            return path.toPath()
        }
        val relativePath = path.removePrefix(root.virtualPath).trimStart('/')
        return if (relativePath.isEmpty()) {
            root.realPath
        } else {
            (root.realPath.toString() + "/$relativePath").toPath()
        }
    }

    override fun toVirtualPath(path: Path): String {
        val text = path.toString()
        val root =
            resolveRoots().firstOrNull { candidate ->
                text == candidate.realPath.toString() || text.startsWith("${candidate.realPath}/")
            } ?: return text
        val relativePath = text.removePrefix(root.realPath.toString()).trimStart('/')
        return if (relativePath.isEmpty()) {
            root.virtualPath
        } else {
            "${root.virtualPath}/$relativePath"
        }
    }

    private fun resolveRoots(): List<IOSResolvedRoot> {
        cachedRoots?.let { return it }
        val storedRoots = loadStoredRoots()
        val validStoredRoots = mutableListOf<IOSStoredRoot>()
        val resolvedRoots = mutableListOf<IOSResolvedRoot>()
        storedRoots.forEach { storedRoot ->
            val bookmarkPath = bookmarkDir / storedRoot.bookmarkFileName
            val bookmarkBytes =
                runCatching {
                    FileSystem.SYSTEM.read(bookmarkPath) {
                        readByteArray()
                    }
                }.getOrNull() ?: return@forEach
            val url = NSURL.resolveBookmark(bookmarkBytes.toNSData()) ?: return@forEach
            if (!url.startAccessingSecurityScopedResource()) return@forEach
            val realPath = url.path ?: return@forEach
            validStoredRoots += storedRoot
            resolvedRoots +=
                IOSResolvedRoot(
                    id = storedRoot.id,
                    name = storedRoot.name,
                    url = url,
                    realPath = realPath.toPath(),
                )
        }
        if (validStoredRoots.size != storedRoots.size) {
            saveStoredRoots(validStoredRoots)
        }
        cachedRoots = resolvedRoots
        return resolvedRoots
    }

    private fun clearResolvedRoots() {
        cachedRoots?.forEach { root ->
            root.url.stopAccessingSecurityScopedResource()
        }
        cachedRoots = null
    }

    private fun loadStoredRoots(): List<IOSStoredRoot> {
        if (!FileSystem.SYSTEM.exists(metadataPath)) return emptyList()
        return runCatching {
            FileSystem.SYSTEM.read(metadataPath) {
                readUtf8().decodeStoredRoots()
            }
        }.getOrDefault(emptyList())
    }

    private fun saveStoredRoots(roots: List<IOSStoredRoot>) {
        FileSystem.SYSTEM.createDirectories(storageDir)
        FileSystem.SYSTEM.createDirectories(bookmarkDir)
        FileSystem.SYSTEM.write(metadataPath) {
            writeUtf8(roots.encodeStoredRoots())
        }
    }

    private suspend fun pickDirectories(): List<NSURL> =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val presenter = topViewController()
                if (presenter == null) {
                    continuation.resume(emptyList())
                    return@suspendCancellableCoroutine
                }
                val picker =
                    UIDocumentPickerViewController(
                        forOpeningContentTypes = listOf(UTTypeFolder),
                        asCopy = false,
                    )
                picker.allowsMultipleSelection = true
                lateinit var delegate: DirectoryPickerDelegate
                delegate =
                    DirectoryPickerDelegate { urls ->
                        PresentationRetainer.release(delegate)
                        continuation.resume(urls)
                    }
                picker.delegate = delegate
                PresentationRetainer.retain(delegate)
                presenter.presentViewController(picker, animated = true, completion = null)
            }
        }
}

private data class IOSStoredRoot(
    val id: String,
    val name: String,
    val bookmarkFileName: String,
)

private data class IOSResolvedRoot(
    val id: String,
    val name: String,
    val url: NSURL,
    val realPath: Path,
) {
    val virtualPath: String = "fs://$id"
}

private class DirectoryPickerDelegate(
    private val onComplete: (List<NSURL>) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onComplete(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onComplete(emptyList())
    }
}

private class FilePreviewCoordinator(
    private val url: NSURL,
) : NSObject(),
    QLPreviewControllerDataSourceProtocol,
    QLPreviewControllerDelegateProtocol {
    private val previewItem = FilePreviewItem(url)

    override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): Long = 1

    override fun previewController(
        controller: QLPreviewController,
        previewItemAtIndex: Long,
    ): QLPreviewItemProtocol = previewItem

    override fun previewControllerWillDismiss(controller: QLPreviewController) {
        FilePreviewPresentationState.activePath = null
        controller.presentingViewController?.dismissViewControllerAnimated(true, completion = null)
        PresentationRetainer.release(this)
    }
}

private object FilePreviewPresentationState {
    var activePath: String? = null
}

private class FilePreviewItem(
    private val url: NSURL,
) : NSObject(),
    QLPreviewItemProtocol {
    override fun previewItemURL(): NSURL = url
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.dataWithBytes(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.bookmarkData(): NSData? =
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        bookmarkDataWithOptions(
            options = 0u,
            includingResourceValuesForKeys = null,
            relativeToURL = null,
            error = error.ptr,
        )
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.Companion.resolveBookmark(data: NSData): NSURL? =
    memScoped {
        val isStale = alloc<BooleanVar>()
        val error = alloc<ObjCObjectVar<NSError?>>()
        URLByResolvingBookmarkData(
            bookmarkData = data,
            options = 0u,
            relativeToURL = null,
            bookmarkDataIsStale = isStale.ptr,
            error = error.ptr,
        )
    }

private fun List<IOSStoredRoot>.encodeStoredRoots(): String =
    joinToString(separator = "\n") { root ->
        listOf(root.id, root.name, root.bookmarkFileName)
            .joinToString(separator = "\t") { value -> value.escapeStoredRootField() }
    }

private fun String.decodeStoredRoots(): List<IOSStoredRoot> =
    lineSequence()
        .filter { line -> line.isNotBlank() }
        .mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size != 3) return@mapNotNull null
            IOSStoredRoot(
                id = fields[0].unescapeStoredRootField(),
                name = fields[1].unescapeStoredRootField(),
                bookmarkFileName = fields[2].unescapeStoredRootField(),
            )
        }.toList()

private fun String.escapeStoredRootField(): String =
    buildString(length) {
        this@escapeStoredRootField.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                else -> append(char)
            }
        }
    }

private fun String.unescapeStoredRootField(): String {
    val source = this
    return buildString(length) {
        var index = 0
        while (index < source.length) {
            val char = source[index]
            if (char == '\\' && index + 1 < source.length) {
                when (val escaped = source[index + 1]) {
                    '\\' -> {
                        append('\\')
                    }

                    't' -> {
                        append('\t')
                    }

                    'n' -> {
                        append('\n')
                    }

                    else -> {
                        append('\\')
                        append(escaped)
                    }
                }
                index += 2
            } else {
                append(char)
                index++
            }
        }
    }
}
