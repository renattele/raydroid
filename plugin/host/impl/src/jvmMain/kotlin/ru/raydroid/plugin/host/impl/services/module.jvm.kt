package ru.raydroid.plugin.host.impl.services

import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.host.impl.DesktopPlatform
import ru.raydroid.plugin.host.impl.services.mac.MacContactsServiceBridgeImpl
import ru.raydroid.plugin.host.impl.services.mac.MacFileSystemGateway
import ru.raydroid.plugin.host.impl.services.mac.MacSystemServiceBridgeImpl

internal actual val platformHostServiceModule =
    module {
        factory<PlatformFileSystemGateway> {
            when (get<DesktopPlatform>()) {
                DesktopPlatform.MAC -> MacFileSystemGateway()
                DesktopPlatform.WINDOWS,
                DesktopPlatform.LINUX,
                DesktopPlatform.OTHER,
                -> PassthroughPlatformFileSystemGateway()
            }
        }
        factory<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
        factory<ContactsServiceBridge> {
            when (get<DesktopPlatform>()) {
                DesktopPlatform.MAC -> MacContactsServiceBridgeImpl()
                DesktopPlatform.WINDOWS,
                DesktopPlatform.LINUX,
                DesktopPlatform.OTHER,
                -> UnsupportedContactsServiceBridge()
            }
        }
        factory<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
        factory<SystemServiceBridge> {
            when (get<DesktopPlatform>()) {
                DesktopPlatform.MAC -> MacSystemServiceBridgeImpl()
                DesktopPlatform.WINDOWS,
                DesktopPlatform.LINUX,
                DesktopPlatform.OTHER,
                -> UnsupportedSystemServiceBridge(get())
            }
        }
    }
