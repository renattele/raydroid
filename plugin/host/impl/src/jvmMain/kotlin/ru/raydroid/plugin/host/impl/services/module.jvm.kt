package ru.raydroid.plugin.host.impl.services

import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.host.impl.DesktopPlatform
import ru.raydroid.plugin.host.impl.detectDesktopPlatform
import ru.raydroid.plugin.host.impl.services.mac.MacSystemServiceBridgeImpl

internal actual val platformHostServiceModule = module {
    single<DesktopPlatform> { detectDesktopPlatform() }
    factory<AllFilesAccessGateway> { UnsupportedAllFilesAccessGateway() }
    factory<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
    factory<ContactsServiceBridge> { UnsupportedContactsServiceBridge() }
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
