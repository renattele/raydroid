package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RaydroidKmpComposeAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyKmpBaseConvention()
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        configureMultiplatformTargetsWhenKmpIsPresent()
        configureKmpAndroidDefaultsWhenPresent()
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    }
}
