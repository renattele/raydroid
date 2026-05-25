package ru.raydroid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.feature.search.DesktopSearchController
import ru.raydroid.search.SearchScreen

private val WindowCornerRadius = 18.dp

@Composable
fun App(desktopSearchController: DesktopSearchController? = null) {
    RaydroidTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(WindowCornerRadius))
                    .background(RaydroidTheme.colorScheme.background),
        ) {
            Column(
                modifier =
                    Modifier
                        .imePadding()
                        .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                SearchScreen(desktopSearchController = desktopSearchController)
            }
        }
    }
}
