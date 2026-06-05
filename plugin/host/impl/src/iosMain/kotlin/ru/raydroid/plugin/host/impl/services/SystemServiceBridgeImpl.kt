package ru.raydroid.plugin.host.impl.services

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge

internal class SystemServiceBridgeImpl : SystemServiceBridge {
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> = emptyList()

    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions,
    ) = Unit

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        val url = NSURL.URLWithString(target) ?: return
        UIApplication.sharedApplication.openURL(url)
    }
}
