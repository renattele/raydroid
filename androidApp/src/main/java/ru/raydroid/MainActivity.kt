package ru.raydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.raydroid.plugin.host.impl.services.AndroidRuntimePermissionGateway

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidRuntimePermissionGateway.attach(this)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(25);
        }
        setContent {
            AndroidApp()
        }
    }

    override fun onDestroy() {
        AndroidRuntimePermissionGateway.detach(this)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (!AndroidRuntimePermissionGateway.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
