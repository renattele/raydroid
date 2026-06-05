package ru.raydroid.core.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import ru.raydroid.core.domain.analytics.AnalyticsLaunchTarget
import ru.raydroid.core.domain.analytics.AnalyticsTracker

internal class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun logLaunch(target: AnalyticsLaunchTarget) {
        firebaseAnalytics.logEvent(target.eventName, null)
    }
}
