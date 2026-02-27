package ru.raydroid.plugin.host.impl.services

import org.koin.dsl.module
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import ru.raydroid.plugin.host.impl.DesktopPlatform
import ru.raydroid.plugin.host.impl.detectDesktopPlatform
import ru.raydroid.plugin.host.impl.services.mac.MacSystemServiceBridgeImpl

internal actual val platformHostServiceModule = module {
    single<DesktopPlatform> { detectDesktopPlatform() }
    factory<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
    factory<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
    factory<SystemServiceBridge> {
        when (get<DesktopPlatform>()) {
            DesktopPlatform.MAC -> MacSystemServiceBridgeImpl()
            DesktopPlatform.WINDOWS -> TODO()
            DesktopPlatform.LINUX -> TODO()
            DesktopPlatform.OTHER -> TODO()
        }
    }
}