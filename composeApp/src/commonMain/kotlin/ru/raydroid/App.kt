package ru.raydroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import ru.raydroid.plugin.api.core.Manifest
import ru.raydroid.plugin.api.core.RayItems
import ru.raydroid.plugin.host.Plugin
import ru.raydroid.plugin.host.ui.ComposeRayItemRenderer
import ru.raydroid.plugin.host.ui.ComposeRayRenderer

@Composable
fun App(field: String, data: RayItems, plugin: Plugin, onFieldUpdate: (String) -> Unit) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(Modifier.heightIn(min = 50.dp).background(MaterialTheme.colorScheme.background).fillMaxWidth()) {
                ComposeRayRenderer(plugin, data.values.flatten())
            }
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focus.requestFocus()
            }
            TextField(field, onFieldUpdate, Modifier.focusRequester(focus).fillMaxWidth())
        }
    }
}
