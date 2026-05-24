package ru.raydroid.plugin.host.impl.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.ui.Icon

internal class SystemServiceBridgeImpl : SystemServiceBridge {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val catalog by lazy {
        loadCatalog().ifEmpty { fallbackCatalog }
    }

    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        val app = UIApplication.sharedApplication
        return catalog
            .asSequence()
            .filter { entry -> entry.enabled }
            .filter { entry -> entry.isAvailable(app) }
            .map { entry ->
                SystemServiceBridge.RawApplication(
                    name = entry.name,
                    id = entry.id,
                    icon = Icon.Builtin(entry.icon),
                )
            }.toList()
    }

    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        val catalogTarget = catalog.firstOrNull { entry -> entry.id == appId }?.url ?: appId
        open(catalogTarget, options)
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions,
    ) {
        val url = NSURL.URLWithString(target) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    private fun loadCatalog(): List<CatalogEntry> {
        val path =
            NSBundle.mainBundle.pathForResource(name = "iOSAppCatalog", ofType = "json")
                ?: return emptyList()
        val content =
            runCatching {
                FileSystem.SYSTEM.read(path.toPath()) {
                    readUtf8()
                }
            }.getOrNull() ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<CatalogEntry>>(content)
        }.getOrElse {
            emptyList()
        }
    }

    @Serializable
    private data class CatalogEntry(
        val id: String,
        val name: String,
        val url: String,
        val icon: String,
        @SerialName("queryScheme") val queryScheme: String? = null,
        val enabled: Boolean = true,
    ) {
        fun isAvailable(application: UIApplication): Boolean {
            val queryUrl = queryUrl() ?: return true
            val url = NSURL.URLWithString(queryUrl) ?: return false
            return application.canOpenURL(url)
        }

        private fun queryUrl(): String? {
            val value = queryScheme?.trim().orEmpty()
            if (value.isEmpty()) return null
            return when {
                value.contains("://") || value.endsWith(":") -> value
                else -> "$value://"
            }
        }
    }

    private companion object {
        val fallbackCatalog =
            listOf(
                CatalogEntry(
                    id = "apple.settings",
                    name = "Settings",
                    url = "App-prefs:",
                    icon = "Tune",
                    queryScheme = "App-prefs:",
                ),
                CatalogEntry(
                    id = "apple.safari",
                    name = "Safari",
                    url = "https://apple.com",
                    icon = "ArrowForward",
                ),
                CatalogEntry(
                    id = "apple.maps",
                    name = "Maps",
                    url = "maps://",
                    icon = "GridView",
                    queryScheme = "maps",
                ),
                CatalogEntry(
                    id = "apple.messages",
                    name = "Messages",
                    url = "sms://",
                    icon = "ArrowForward",
                    queryScheme = "sms",
                ),
            )
    }
}
