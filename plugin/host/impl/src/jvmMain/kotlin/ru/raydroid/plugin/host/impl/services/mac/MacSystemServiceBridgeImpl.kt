package ru.raydroid.plugin.host.impl.services.mac

import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import java.io.File

internal class MacSystemServiceBridgeImpl: SystemServiceBridge {



    override suspend fun open(
        app: SystemServiceBridge.RawApplication,
        options: SystemServiceBridge.OpenOptions
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        val dirs = listOf(
            "/Applications",
            "/System/Applications",
            "${System.getProperty("user.home")}/Applications"
        )

        return dirs.flatMap { path ->
            File(path)
                .listFiles { file -> file.extension == "app" }
                ?.mapNotNull { appDir ->
                    val infoPlist = File(appDir, "Contents/Info.plist")
                    if (!infoPlist.exists()) return@mapNotNull null

                    val bundleId = readPlistValue(infoPlist, "CFBundleIdentifier")
                    val appName = readPlistValue(infoPlist, "CFBundleName")
                        ?: appDir.nameWithoutExtension

                    if (bundleId != null) {
                        SystemServiceBridge.RawApplication(appName, bundleId)
                    } else null
                } ?: emptyList()
        }
    }

    private fun readPlistValue(plist: File, key: String): String? {
        return try {
            val process = ProcessBuilder(
                "plutil",
                "-extract", key,
                "raw",
                "-o", "-",
                plist.absolutePath
            ).start()

            process.inputStream.bufferedReader().readText().trim()
                .ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}