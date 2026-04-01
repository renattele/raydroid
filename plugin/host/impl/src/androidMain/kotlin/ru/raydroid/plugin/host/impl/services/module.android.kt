package ru.raydroid.plugin.host.impl.services

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.raydroid.plugin.api.host.transport.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.transport.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge

internal actual val platformHostServiceModule = module {
    factory<ClipboardServiceBridge> { ClipboardServiceBridgeImpl(androidContext()) }
    factory<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
    factory<SystemServiceBridge> { SystemServiceBridgeImpl(androidContext()) }
}