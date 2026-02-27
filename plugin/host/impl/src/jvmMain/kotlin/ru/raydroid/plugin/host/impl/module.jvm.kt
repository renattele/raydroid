package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import ru.raydroid.plugin.host.impl.services.ClipboardServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.EnvironmentServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.mac.MacSystemServiceBridgeImpl
import java.io.File
import java.util.concurrent.Executors

private fun getLocalFilesPath(): Path {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    val baseDir = when {
        os.contains("win") -> System.getenv("APPDATA")
        os.contains("mac") -> "$userHome/Library/Application Support"
        else -> "$userHome/.local/share"
    }

    val appDir = File(baseDir, "Raydroid")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    return appDir.absolutePath.toPath()
}

internal actual val pluginPlatformModule = module {
    single<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
    single<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
    single<SystemServiceBridge> { MacSystemServiceBridgeImpl() }
    single<FileSystem>(named("localFileSystem")) {
        FileSystem.SYSTEM
    }
    single<Path>(named("localPath")) {
        getLocalFilesPath()
    }
    factory<CoroutineDispatcher> {
        Executors.newSingleThreadExecutor {
            Thread(
                /* group = */ null,
                /* task = */ it,
                /* name = */ "Zipline Executor",
                /* stackSize = */ 256000
            )
        }.asCoroutineDispatcher()
    }
}