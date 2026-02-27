package ru.raydroid.plugin.host.impl.services

import org.koin.dsl.module
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge
import ru.raydroid.plugin.api.host.bridge.EnvironmentServiceBridge
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge

internal actual val platformHostServiceModule = module {
    factory<ClipboardServiceBridge> { ClipboardServiceBridgeImpl() }
    factory<EnvironmentServiceBridge> { EnvironmentServiceBridgeImpl() }
    factory<SystemServiceBridge> { SystemServiceBridgeImpl() }}