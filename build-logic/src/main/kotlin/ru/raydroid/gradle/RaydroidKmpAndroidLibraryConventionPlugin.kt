package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RaydroidKmpAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            applyKmpBaseConvention()
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            configureKmpAndroidDefaultsWhenPresent()
        }
}
