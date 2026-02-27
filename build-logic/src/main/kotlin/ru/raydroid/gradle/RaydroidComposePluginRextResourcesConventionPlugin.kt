package ru.raydroid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import javax.inject.Inject

abstract class PreparePluginComposeResourcesTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Input
    abstract val pluginIds: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rextFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        fileSystemOperations.sync {
            into(outputDir)

            into("files") {
                from(rextFiles)
            }
        }

        val pluginListFile = outputDir.file("files/plugin-list.json").get().asFile
        pluginListFile.parentFile.mkdirs()
        pluginListFile.writeText(pluginIds.asJsonArray())
    }
}

private fun ListProperty<String>.asJsonArray(): String {
    return get().joinToString(
        prefix = "[\n",
        postfix = "\n]\n",
        separator = ",\n",
    ) { pluginId ->
        "  \"$pluginId\""
    }
}

class RaydroidComposePluginRextResourcesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.withPlugin("org.jetbrains.compose") {
            configurePluginRextComposeResources()
        }
    }
}

private fun Project.configurePluginRextComposeResources() {
    val requestedTaskNames = gradle.startParameter.taskNames.joinToString(" ").lowercase()
    val pluginBuildOutputVariant = providers.gradleProperty("raydroidPluginVariant")
        .orNull
        ?.lowercase()
        ?: if (requestedTaskNames.contains("release") || requestedTaskNames.contains("production")) {
            "production"
        } else {
            "development"
        }

    require(pluginBuildOutputVariant in setOf("development", "production")) {
        "raydroidPluginVariant must be 'development' or 'production', got '$pluginBuildOutputVariant'"
    }

    val pluginBuildTaskVariant = pluginBuildOutputVariant.replaceFirstChar { it.uppercase() }
    val pluginImplProjects = rootProject.subprojects.filter { it.path.startsWith(":plugin:impl:") }
    val pluginIds = pluginImplProjects.map { it.pluginPackageName() }

    val preparePluginComposeResources = tasks.register<PreparePluginComposeResourcesTask>("preparePluginComposeResources") {
        group = "build setup"
        description = "Builds plugin .rext artifacts and copies them into generated Compose resources"

        dependsOn(pluginImplProjects.map { "${it.path}:raydroidPlugin${pluginBuildTaskVariant}Build" })
        this.pluginIds.set(pluginIds)

        rextFiles.from(
            pluginImplProjects.zip(pluginIds).map { (pluginProject, pluginId) ->
                pluginProject.layout.buildDirectory.file("output/$pluginBuildOutputVariant/$pluginId.rext")
            }
        )
        outputDir.set(layout.buildDirectory.dir("generated/composeResources/pluginRext/$pluginBuildOutputVariant"))
    }

    val composeExtension = extensions.getByType(ComposeExtension::class.java) as ExtensionAware
    composeExtension.extensions.configure<ResourcesExtension> {
        customDirectory(
            sourceSetName = "commonMain",
            directoryProvider = preparePluginComposeResources.flatMap { it.outputDir },
        )
    }

    tasks.named("build").configure {
        dependsOn(preparePluginComposeResources)
    }
}
