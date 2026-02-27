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
import ru.raydroid.plugin.host.impl.services.SystemServiceBridgeImpl
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal actual val pluginPlatformModule = module {
    single<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
    single<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
    single<SystemServiceBridge> { SystemServiceBridgeImpl() }
    single<FileSystem>(named("localFileSystem")) {
        FileSystem.SYSTEM
    }
    single<Path>(named("localPath")) {
        System.getProperty("user.dir").toPath()
    }
    factory<CoroutineDispatcher> {
        Executors.newSingleThreadExecutor {
            Thread(
                /* group = */ null,
                /* task = */ it,
                /* name = */ "Zipline Executor",
                /* stackSize = */ 512000
            )
        }.asCoroutineDispatcher()
    }
}