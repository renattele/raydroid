package ru.raydroid.core.data.analytics

import ru.raydroid.core.domain.analytics.AnalyticsTracker
import ru.raydroid.core.domain.analytics.NoOpAnalyticsTracker

private var platformAnalyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker

object FirebaseAnalyticsBridge {
    fun register(tracker: AnalyticsTracker) {
        platformAnalyticsTracker = tracker
    }

    fun clear() {
        platformAnalyticsTracker = NoOpAnalyticsTracker
    }
}

internal fun resolveAnalyticsTracker(): AnalyticsTracker = platformAnalyticsTracker
