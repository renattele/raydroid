package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.host.impl.services.ClipboardServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.EnvironmentServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.SystemServiceBridgeImpl

private fun getLocalFilesPath(): Path {
    val documents =
        NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String ?: ""
    return documents.toPath()
}

internal actual val pluginPlatformModule =
    module {
        single<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
        single<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
        single<SystemServiceBridge> { SystemServiceBridgeImpl() }
        single<Platform>(named("hostPlatform")) { Platform.IOS }
        single<FileSystem>(named("localFileSystem")) {
            FileSystem.SYSTEM
        }
        single<Path>(named("localPath")) {
            getLocalFilesPath()
        }
        factory<CoroutineDispatcher> {
            Dispatchers.Main
        }
    }
