package ru.raydroid.plugin.host.impl.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import ru.raydroid.plugin.api.host.bridge.SystemServiceBridge
import java.net.URLConnection
import androidx.core.net.toUri

internal class SystemServiceBridgeImpl(
    private val activityContext: Context
): SystemServiceBridge {
    private val packageManager by lazy {
        activityContext.packageManager
    }
    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return installedApps.map { app ->
            SystemServiceBridge.RawApplication(
                name = app.name,
                id = app.packageName
            )
        }
    }

    override suspend fun open(
        app: SystemServiceBridge.RawApplication,
        options: SystemServiceBridge.OpenOptions
    ) {
        val intent = packageManager.getLaunchIntentForPackage(app.id) ?: return
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        activityContext.startActivity(intent)
    }

    override suspend fun open(
        target: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        val contentType = URLConnection.guessContentTypeFromName(target)
        val intent = Intent().apply {
            setAction(Intent.ACTION_VIEW)
            setDataAndType(contentType.toUri(), contentType)
            applyOptions(options)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun Intent.applyOptions(options: SystemServiceBridge.OpenOptions) {
        extras?.apply {
            options.keys.forEach { (key, value) ->
                putString(key, value)
            }
        }
    }
}