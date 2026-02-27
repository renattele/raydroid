package ru.raydroid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class RaydroidPluginConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("raydroid.zipline")

        registerPluginVariantTasks(
            variantName = "Development",
            outputVariantName = "development",
        )
        registerPluginVariantTasks(
            variantName = "Production",
            outputVariantName = "production",
        )
    }
}

private fun Project.registerPluginVariantTasks(
    variantName: String,
    outputVariantName: String,
) {
    val compileTaskName = "compile${variantName}ExecutableKotlinJsZipline"
    val serveTaskName = "serve${variantName}Zipline"
    val buildTaskName = "raydroidPlugin${variantName}Build"
    val serveAliasTaskName = "raydroidPlugin${variantName}Serve"

    tasks.register<Zip>(buildTaskName) {
        group = "raydroid plugin"
        description = "Compiles and packages $variantName Zipline output as .rext"

        dependsOn(compileTaskName)
        from(layout.buildDirectory.dir("zipline/$variantName"))
        from(layout.projectDirectory.dir("src/jsMain/resources")) {
            into("resources")
            includeEmptyDirs = true
        }

        destinationDirectory.set(layout.buildDirectory.dir("output/$outputVariantName"))
        archiveFileName.set("${pluginPackageName()}.rext")
    }

    tasks.register<DefaultTask>(serveAliasTaskName) {
        group = "raydroid plugin"
        description = "Alias for $serveTaskName"
        dependsOn(serveTaskName)
    }
}
