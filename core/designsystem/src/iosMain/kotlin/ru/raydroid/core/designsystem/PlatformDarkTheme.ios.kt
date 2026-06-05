package ru.raydroid.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformIsSystemInDarkTheme(): Boolean = isSystemInDarkTheme()
