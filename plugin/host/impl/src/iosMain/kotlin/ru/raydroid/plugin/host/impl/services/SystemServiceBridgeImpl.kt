package ru.raydroid.plugin.host.impl.services

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.ui.Icon

internal class SystemServiceBridgeImpl : SystemServiceBridge {
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        val app = UIApplication.sharedApplication
        return KnownApps
            .filter { knownApp ->
                val url = NSURL.URLWithString(knownApp.url) ?: return@filter false
                app.canOpenURL(url)
            }
            .map { knownApp ->
                SystemServiceBridge.RawApplication(
                    name = knownApp.name,
                    id = knownApp.id,
                    icon = Icon.Builtin(knownApp.icon)
                )
            }
    }

    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        val knownTarget = KnownApps.firstOrNull { it.id == appId }?.url ?: appId
        open(knownTarget, options)
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        val url = NSURL.URLWithString(target) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    private data class KnownApp(
        val id: String,
        val name: String,
        val url: String,
        val icon: String
    )

    private companion object {
        val KnownApps = listOf(
            KnownApp(id = "apple.settings", name = "Settings", url = "App-prefs:", icon = "Tune"),
            KnownApp(id = "apple.safari", name = "Safari", url = "https://apple.com", icon = "ArrowForward"),
            KnownApp(id = "apple.maps", name = "Maps", url = "maps://", icon = "GridView"),
            KnownApp(id = "apple.mail", name = "Mail", url = "mailto:", icon = "HelpOutline"),
            KnownApp(id = "apple.music", name = "Music", url = "music://", icon = "Calculate"),
            KnownApp(id = "apple.facetime", name = "FaceTime", url = "facetime://", icon = "ArrowForward"),
            KnownApp(id = "apple.phone", name = "Phone", url = "tel://", icon = "ArrowForward"),
            KnownApp(id = "apple.messages", name = "Messages", url = "sms://", icon = "ArrowForward")
        )
    }
}
