package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

class RaydroidKmpComposeAndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
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

        val outputDirectoryGetter = javaClass.methods.firstOrNull { method ->
            method.name == "getOutputDirectory" && method.parameterCount == 0
        } ?: return@configureEach

        val outputDirectory = outputDirectoryGetter.invoke(this) as? DirectoryProperty ?: return@configureEach
        outputDirectory.convention(
            layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/$name")
        )
    }

    tasks.configureEach {
        if (name != "bundleAndroidMainAar") return@configureEach
        val bundleTask = this as? Zip ?: return@configureEach
        val copyTask = tasks.named("copyAndroidMainComposeResourcesToAndroidAssets")
        val copyTaskOutput = copyTask.flatMap { task ->
            val outputDirectoryGetter = task.javaClass.methods.firstOrNull { method ->
                method.name == "getOutputDirectory" && method.parameterCount == 0
            } ?: error("copyAndroidMainComposeResourcesToAndroidAssets must expose outputDirectory")

            val outputDirectory = outputDirectoryGetter.invoke(task) as? DirectoryProperty
                ?: error("copyAndroidMainComposeResourcesToAndroidAssets outputDirectory has unexpected type")
            outputDirectory
        }

        bundleTask.dependsOn(copyTask)
        bundleTask.with(copySpec {
            from(copyTaskOutput)
            into("assets")
        })
    }
}
