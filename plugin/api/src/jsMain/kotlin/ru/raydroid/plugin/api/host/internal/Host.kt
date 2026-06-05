package ru.raydroid.plugin.api.host

import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.host.internal.HostServiceImpl
import ru.raydroid.plugin.api.host.service.HostService
import ru.raydroid.plugin.api.host.transport.HostServiceBridge
import ru.raydroid.plugin.api.zipline

private val HostBridge: HostServiceBridge by lazy { zipline.take(ZiplineServices.Host.toString()) }

val Host: HostService by lazy {
    HostServiceImpl(HostBridge)
}
