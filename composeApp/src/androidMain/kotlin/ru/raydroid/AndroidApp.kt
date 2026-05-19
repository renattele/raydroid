package ru.raydroid

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.raydroid.core.designsystem.RaydroidTheme
import ru.raydroid.core.designsystem.component.RPopupSurface
import ru.raydroid.search.SearchScreen

@Composable
fun AndroidApp() {
    RaydroidTheme {
        val activity = LocalContext.current as? Activity
        val dismissInteractionSource = remember { MutableInteractionSource() }
        val surfaceShape = RoundedCornerShape(
            topStart = RaydroidTheme.spacing.extraLarge,
            topEnd = RaydroidTheme.spacing.extraLarge,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val panelMaxHeight = maxHeight * 0.82f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                    )
                    .background(RaydroidTheme.colorScheme.background)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismissInteractionSource,
                        indication = null
                    ) {
                        activity?.finish()
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                RPopupSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = panelMaxHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .clip(surfaceShape),
                    shape = surfaceShape,
                    color = RaydroidTheme.colorScheme.background,
                    shadowColor = RaydroidTheme.colorScheme.scrim.copy(alpha = 0.55f)
                ) {
                    SearchScreen(Modifier.fillMaxSize())
                }
            }
        }
    }
}
