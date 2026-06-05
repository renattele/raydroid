package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

class RaydroidKmpComposeAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            applyKmpBaseConvention()
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            configureMultiplatformTargetsWhenKmpIsPresent()
            configureKmpAndroidDefaultsWhenPresent()
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            configureComposeResources()
            configureAndroidComposeResourceTaskWorkaround()
        }
}

private fun Project.configureComposeResources() {
    val extension = extensions.getByType(ComposeExtension::class.java) as ExtensionAware
    extension.extensions.configure<ResourcesExtension> {
        this.publicResClass = true
    }
}

private fun Project.configureAndroidComposeResourceTaskWorkaround() {
    tasks.configureEach {
        if (!name.startsWith("copy") || !name.endsWith("ComposeResourcesToAndroidAssets")) return@configureEach

        val outputDirectoryGetter =
            javaClass.methods.firstOrNull { method ->
                method.name == "getOutputDirectory" && method.parameterCount == 0
            } ?: return@configureEach

        val outputDirectory = outputDirectoryGetter.invoke(this) as? DirectoryProperty ?: return@configureEach
        outputDirectory.convention(
            layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/$name"),
        )
    }
}
