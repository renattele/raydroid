package ru.raydroid.core.domain.analytics

interface AnalyticsTracker {
    fun logLaunch(target: AnalyticsLaunchTarget)
}

enum class AnalyticsLaunchTarget(
    val eventName: String,
    val pluginId: String? = null,
) {
    Home("launch_home"),
    Calculator("launch_calculator", "ru.raydroid.plugin.impl.calculator"),
    Notes("launch_notes", "ru.raydroid.plugin.impl.notes"),
    Weather("launch_weather", "ru.raydroid.plugin.impl.weather"),
    Files("launch_files", "ru.raydroid.plugin.impl.files"),
    Contacts("launch_contacts", "ru.raydroid.plugin.impl.contacts"),
    ;

    companion object {
        fun fromPluginId(pluginId: String): AnalyticsLaunchTarget? = entries.firstOrNull { target -> target.pluginId == pluginId }
    }
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun logLaunch(target: AnalyticsLaunchTarget) = Unit
}
