package ru.raydroid.core.designsystem

import android.animation.ValueAnimator

internal actual fun platformPrefersReducedMotion(): Boolean = !ValueAnimator.areAnimatorsEnabled()
