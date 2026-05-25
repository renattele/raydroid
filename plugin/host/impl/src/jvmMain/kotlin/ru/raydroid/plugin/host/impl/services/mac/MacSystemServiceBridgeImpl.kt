package ru.raydroid.plugin.host.impl.services.mac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.ui.Icon
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.filechooser.FileSystemView

internal class MacSystemServiceBridgeImpl(
    private val commandExecutor: (List<String>) -> Unit = ::runCommand,
    private val applicationDirs: List<String> = DefaultApplicationDirs,
) : SystemServiceBridge {
    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        withContext(Dispatchers.IO) {
            val appDir = findAppDirectory(appId)
            val command =
                if (appDir != null) {
                    listOf("open", "-a", appDir.absolutePath)
                } else {
                    listOf("open", "-b", appId)
                }

            commandExecutor(command)
        }
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        withContext(Dispatchers.IO) {
            commandExecutor(
                listOf(
                    "open",
                    normalizeOpenTarget(target),
                ),
            )
        }
    }

    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> =
        withContext(Dispatchers.IO) {
            applicationDirs.flatMap { path ->
                File(path)
                    .listFiles { file -> file.extension == "app" }
                    ?.mapNotNull { appDir ->
                        val infoPlist = File(appDir, "Contents/Info.plist")
                        if (!infoPlist.exists()) return@mapNotNull null

                        val bundleId = readPlistValue(infoPlist, "CFBundleIdentifier")
                        val appName =
                            readPlistValue(infoPlist, "CFBundleName")
                                ?: appDir.nameWithoutExtension

                        if (bundleId != null) {
                            SystemServiceBridge.RawApplication(
                                name = appName,
                                id = bundleId,
                                icon = resolveIcon(appDir, infoPlist, bundleId),
                            )
                        } else {
                            null
                        }
                    } ?: emptyList()
            }.distinctBy { app -> app.id }
        }

    private fun findAppDirectory(appId: String): File? =
        applicationDirs
            .asSequence()
            .map(::File)
            .filter(File::exists)
            .flatMap { dir ->
                dir
                    .listFiles { file -> file.extension == "app" }
                    .orEmpty()
                    .asSequence()
            }.firstOrNull { appDir ->
                val infoPlist = File(appDir, "Contents/Info.plist")
                infoPlist.exists() && readPlistValue(infoPlist, "CFBundleIdentifier") == appId
            }

    private fun resolveIcon(
        appDir: File,
        infoPlist: File,
        bundleId: String,
    ): Icon {
        val resolvedFile =
            resolveIconFile(appDir, infoPlist)
                ?.let { resolveRenderableIconFile(it, bundleId) }
                ?: resolveSystemIconFile(appDir, bundleId)
                ?: resolveRenderableIconFile(genericApplicationIconFile, "generic")
                ?: genericApplicationIconFile

        val base64 =
            runCatching {
                Base64.getEncoder().encodeToString(resolvedFile.readBytes())
            }.getOrNull()
        return if (base64 != null) {
            Icon.Url("data:image/png;base64,$base64")
        } else {
            Icon.Url(resolvedFile.toURI().toString())
        }
    }

    private fun resolveIconFile(
        appDir: File,
        infoPlist: File,
    ): File? {
        val resourcesDir = File(appDir, "Contents/Resources")
        if (!resourcesDir.exists()) return null

        val iconNames =
            listOfNotNull(
                readPlistValue(infoPlist, "CFBundleIconFile"),
                readPlistValue(infoPlist, "CFBundleIconName"),
                readPlistValue(infoPlist, "CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles.0"),
            ).distinct()

        return iconNames
            .asSequence()
            .flatMap { iconName -> iconCandidates(resourcesDir, iconName).asSequence() }
            .firstOrNull(File::exists)
            ?: resourcesDir
                .walkTopDown()
                .filter(File::isFile)
                .firstOrNull { file -> file.matchesIconName(iconNames) }
    }

    private fun iconCandidates(
        resourcesDir: File,
        iconName: String,
    ): List<File> =
        if (iconName.contains('.')) {
            listOf(File(resourcesDir, iconName))
        } else {
            listOf(
                File(resourcesDir, iconName),
                File(resourcesDir, "$iconName.icns"),
                File(resourcesDir, "$iconName.png"),
            )
        }

    private fun resolveRenderableIconFile(
        iconFile: File,
        cacheKey: String,
    ): File? {
        if (!iconFile.exists()) return null
        if (iconFile.extension.equals("png", ignoreCase = true)) return iconFile

        val cachedIconFile = File(iconCacheDir, "${cacheKey.sanitizeForFileName()}.png")
        if (cachedIconFile.exists() && cachedIconFile.lastModified() >= iconFile.lastModified()) {
            return cachedIconFile
        }

        cachedIconFile.parentFile?.mkdirs()

        return try {
            val process =
                ProcessBuilder(
                    "sips",
                    "-s",
                    "format",
                    "png",
                    iconFile.absolutePath,
                    "--out",
                    cachedIconFile.absolutePath,
                ).redirectErrorStream(true)
                    .start()

            process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            cachedIconFile.takeIf { exitCode == 0 && it.exists() }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSystemIconFile(
        appDir: File,
        cacheKey: String,
    ): File? {
        val cachedIconFile = File(iconCacheDir, "${cacheKey.sanitizeForFileName()}-finder.png")
        if (cachedIconFile.exists() && cachedIconFile.lastModified() >= appDir.lastModified()) {
            return cachedIconFile
        }

        cachedIconFile.parentFile?.mkdirs()

        return try {
            val systemIcon = FileSystemView.getFileSystemView().getSystemIcon(appDir) ?: return null
            val width = systemIcon.iconWidth.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE_PX
            val height = systemIcon.iconHeight.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE_PX
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                systemIcon.paintIcon(null, graphics, 0, 0)
            } finally {
                graphics.dispose()
            }

            cachedIconFile.takeIf { ImageIO.write(image, "png", it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun readPlistValue(
        plist: File,
        key: String,
    ): String? {
        return try {
            val process =
                ProcessBuilder(
                    "plutil",
                    "-extract",
                    key,
                    "raw",
                    "-o",
                    "-",
                    plist.absolutePath,
                ).redirectErrorStream(true)
                    .start()

            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            if (process.waitFor() != 0) return null

            output
                .ifBlank { null }
                ?.takeUnless { it == "null" }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.sanitizeForFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun File.matchesIconName(iconNames: List<String>): Boolean =
        iconNames.any { iconName ->
            name.equals(iconName, ignoreCase = true) ||
                nameWithoutExtension.equals(iconName, ignoreCase = true)
        }

    private companion object {
        private const val DEFAULT_ICON_SIZE_PX = 256
        private val DefaultApplicationDirs =
            listOf(
                "/Applications",
                "/System/Applications",
                "${System.getProperty("user.home")}/Applications",
            )

        private val iconCacheDir =
            File(
                System.getProperty("java.io.tmpdir"),
                "raydroid/system-app-icons",
            )

        private val genericApplicationIconFile =
            File(
                "/System/Library/CoreServices/CoreTypes.bundle/Contents/Resources/GenericApplicationIcon.icns",
            )
    }
}

internal fun normalizeOpenTarget(target: String): String =
    when {
        target.startsWith("file://") -> {
            runCatching { File(URI(target)).absolutePath }
                .getOrElse { target.removePrefix("file://") }
        }

        File(target).isAbsolute -> File(target).absolutePath
        else -> target
    }

internal fun runCommand(command: List<String>) {
    try {
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
    } catch (_: Exception) {
        // Best-effort bridge behavior matches other platforms.
    }
}
