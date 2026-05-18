package ru.raydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import ru.raydroid.plugin.host.impl.services.AndroidRuntimePermissionGateway

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidRuntimePermissionGateway.attach(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            App()
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
