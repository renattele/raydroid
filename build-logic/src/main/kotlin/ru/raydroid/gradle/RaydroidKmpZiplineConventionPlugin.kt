package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RaydroidKmpZiplineConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            applyKmpBaseConvention()
            pluginManager.apply("app.cash.zipline")
        }
}
