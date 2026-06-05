package ru.raydroid.plugin.host.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.manifest.Platform
import ru.raydroid.plugin.host.impl.services.ClipboardServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.EnvironmentServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.SystemServiceBridgeImpl
import java.util.concurrent.Executors

internal actual val pluginPlatformModule =
    module {
        single<ClipboardServiceBridge> { ClipboardServiceBridgeImpl(get()) }
        single<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
        single<SystemServiceBridge> { SystemServiceBridgeImpl(get()) }
        single<Platform>(named("hostPlatform")) { Platform.Android }

        single<FileSystem>(named("localFileSystem")) {
            FileSystem.SYSTEM
        }
        single<Path>(named("localPath")) {
            androidContext().filesDir.absolutePath.toPath()
        }
        factory<CoroutineDispatcher> {
            Executors
                .newSingleThreadExecutor {
                    Thread(
                        // group =
                        null,
                        // target =
                        it,
                        // name =
                        "Zipline Executor",
                        // stackSize =
                        512000,
                    )
                }.asCoroutineDispatcher()
        }
    }
