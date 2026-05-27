package ru.raydroid.core.data.analytics

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.raydroid.core.domain.analytics.AnalyticsTracker

internal actual val analyticsPlatformModule: Module =
    module {
        single<AnalyticsTracker> { resolveAnalyticsTracker() }
    }
