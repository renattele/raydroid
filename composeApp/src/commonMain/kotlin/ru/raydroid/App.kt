package ru.raydroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.ui.ComposeRayItemRenderer

@Composable
fun App(field: String, data: State<RayItems>, onFieldUpdate: (String) -> Unit) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            ComposeRayItemRenderer(data)
            TextField(field, onFieldUpdate, Modifier.fillMaxWidth())
        }
    }
}