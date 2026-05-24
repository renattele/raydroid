package ru.raydroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.search.SearchScreen

@Composable
fun App() {
    RaydroidTheme {
        Column(
            modifier =
                Modifier
                    .imePadding()
                    .fillMaxSize()
                    .background(RaydroidTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            SearchScreen()
        }
    }
}
