package ru.raydroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.raydroid.plugin.host.impl.presentation.ThemeProvider
import ru.raydroid.search.SearchScreen

@Composable
fun App() {
    MaterialTheme {
        ThemeProvider {
            Column(
                modifier = Modifier
                    .imePadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                SearchScreen()
            }
        }
    }
}
