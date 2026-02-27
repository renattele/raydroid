package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RaydroidMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyKmpBaseConvention()
        configureMultiplatformTargetsWhenKmpIsPresent()
    }
}
