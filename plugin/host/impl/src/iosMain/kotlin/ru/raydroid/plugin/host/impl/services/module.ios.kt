package ru.raydroid.plugin.host.impl.services

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal actual val platformHostServiceModule: Module
    get() =
        module {
            factory<PlatformFileSystemGateway> { IOSFileSystemGateway(basePath = get(named("localPath"))) }
            factory<ContactsServiceBridge> { IOSContactsServiceBridgeImpl() }
        }
