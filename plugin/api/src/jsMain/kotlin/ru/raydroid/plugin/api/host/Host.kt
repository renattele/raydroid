package ru.raydroid.plugin.api.host

import ru.raydroid.plugin.api.ZiplineServices
import ru.raydroid.plugin.api.zipline


val Host: HostBridge by lazy { zipline.take(ZiplineServices.Host.toString()) }