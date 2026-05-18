package ru.raydroid.plugin.host.impl.services

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ContactsServiceBridge

internal actual val platformHostServiceModule: Module
    get() = module {
        factory<AllFilesAccessGateway> { UnsupportedAllFilesAccessGateway() }
        factory<ContactsServiceBridge> { UnsupportedContactsServiceBridge() }
    }
