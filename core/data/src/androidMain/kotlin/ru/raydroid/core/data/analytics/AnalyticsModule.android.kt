package ru.raydroid.core.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.raydroid.core.domain.analytics.AnalyticsTracker

internal actual val analyticsPlatformModule: Module =
    module {
        single<FirebaseAnalytics> { FirebaseAnalytics.getInstance(androidContext()) }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker(get()) }
    }
