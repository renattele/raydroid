package ru.raydroid.plugin.host.impl.services

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.raydroid.plugin.api.host.transport.SystemServiceBridge
import ru.raydroid.plugin.api.ui.Icon
import java.io.ByteArrayOutputStream
import java.net.URLConnection

internal class SystemServiceBridgeImpl(
    private val activityContext: Context
): SystemServiceBridge {
    private val packageManager by lazy {
        activityContext.packageManager
    }

    override suspend fun getApps(): List<SystemServiceBridge.RawApplication> = withContext(Dispatchers.Default) {
        queryLaunchableApps()
            .distinctBy { it.activityInfo?.packageName }
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                SystemServiceBridge.RawApplication(
                    name = resolveName(resolveInfo, packageName),
                    id = packageName,
                    icon = resolveIcon(resolveInfo)
                )
            }
    }

    override suspend fun openApp(
        appId: String,
        options: SystemServiceBridge.OpenOptions
    ) {
        val intent = packageManager.getLaunchIntentForPackage(appId) ?: return
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

    private fun queryLaunchableApps(): List<ResolveInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(launcherIntent, 0)
    }

    private fun resolveName(resolveInfo: ResolveInfo, packageName: String): String {
        return resolveInfo.loadLabel(packageManager)
            .normalizeLabel()
            ?: resolveInfo.activityInfo?.applicationInfo
                ?.loadLabel(packageManager)
                .normalizeLabel()
            ?: packageName
    }

    private fun resolveIcon(resolveInfo: ResolveInfo): Icon {
        val drawable = try {
            resolveInfo.loadIcon(packageManager)
        } catch (_: Exception) {
            try {
                resolveInfo.activityInfo?.applicationInfo?.loadIcon(packageManager)
                    ?: packageManager.defaultActivityIcon
            } catch (_: Exception) {
                packageManager.defaultActivityIcon
            }
        }

        val base64 = drawable
            .toBitmap()
            .toBase64Png()
            ?: return Icon.Url(DEFAULT_APP_ICON_URI)

        return Icon.Url("data:image/png;base64,$base64")
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap
        }

        val width = intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE_PX
        val height = intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_ICON_SIZE_PX
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    private fun Bitmap.toBase64Png(): String? {
        return try {
            val bytes = ByteArrayOutputStream().use { output ->
                compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun CharSequence?.normalizeLabel(): String? {
        return this?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        private const val DEFAULT_ICON_SIZE_PX = 128
        private const val DEFAULT_APP_ICON_URI = "android.resource://android/${android.R.mipmap.sym_def_app_icon}"
    }
}
