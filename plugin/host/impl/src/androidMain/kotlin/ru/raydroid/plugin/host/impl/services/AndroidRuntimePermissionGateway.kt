package ru.raydroid.plugin.host.impl.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AndroidRuntimePermissionGateway {
    private var activityRef: WeakReference<ComponentActivity>? = null
    private val pendingRequests = mutableMapOf<Int, () -> Unit>()
    private var nextRequestCode = 7000

    fun attach(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
    }

    fun detach(activity: ComponentActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    suspend fun requestReadContacts(context: Context) {
        requestPermission(context, Manifest.permission.READ_CONTACTS)
    }

    suspend fun requestCallPhone(context: Context) {
        requestPermission(context, Manifest.permission.CALL_PHONE)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ): Boolean {
        val complete = pendingRequests.remove(requestCode) ?: return false
        complete()
        return true
    }

    private suspend fun requestPermission(
        context: Context,
        permission: String,
    ) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        val activity = activityRef?.get() ?: return
        suspendCoroutine { continuation ->
            val requestCode = nextRequestCode++
            pendingRequests[requestCode] = { continuation.resume(Unit) }
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }
}
