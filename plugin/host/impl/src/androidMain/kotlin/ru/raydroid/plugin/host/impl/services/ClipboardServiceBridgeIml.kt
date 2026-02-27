package ru.raydroid.plugin.host.impl.services

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.core.net.toFile
import androidx.core.net.toUri
import ru.raydroid.plugin.api.host.bridge.ClipboardServiceBridge


internal class ClipboardServiceBridgeImpl(
    private val context: Context
): ClipboardServiceBridge {
    private val clipboardManager by lazy {
        context.getSystemService(ClipboardManager::class.java)
    }
    override suspend fun copy(
        content: ClipboardServiceBridge.ClipboardContent,
        secret: Boolean
    ) {
        val clip = if (content.text != null) {
            ClipData.newPlainText("text", content.text)
        } else if (content.filePath != null) {
            ClipData.newUri(context.contentResolver, "path", content.filePath!!.toUri())
        } else {
            return
        }
        clip.description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, secret)
            }
            else {
                putBoolean("android.content.extra.IS_SENSITIVE", secret)
            }
        }
        clipboardManager.setPrimaryClip(clip)
    }

    override suspend fun clear() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, ""))
    }

    override suspend fun read(historyOffset: Int): ClipboardServiceBridge.ClipboardContent {
        val clip = clipboardManager.primaryClip ?: return ClipboardServiceBridge.ClipboardContent()
        val item = clip.getItemAt(0) ?: return ClipboardServiceBridge.ClipboardContent()
        return ClipboardServiceBridge.ClipboardContent(
            text = item.text?.toString(),
            filePath = item.uri?.toFile()?.absolutePath
        )
    }
}