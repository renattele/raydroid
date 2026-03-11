package ru.raydroid.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Sources
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

abstract class CollectComposeDependencyAssetsTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assetDirs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        fileSystemOperations.sync {
            includeEmptyDirs = false
            from(assetDirs)
            into(outputDir)
        }
    }
}

class RaydroidAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = libsCatalog()

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("android-compileSdk")

            defaultConfig {
                minSdk = libs.versionInt("android-minSdk")
                targetSdk = libs.versionInt("android-targetSdk")
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }

        configureAndroidComposeDependencyAssets()
    }
}

private fun Project.configureAndroidComposeDependencyAssets() {
    val collectTasks = mutableListOf<org.gradle.api.tasks.TaskProvider<CollectComposeDependencyAssetsTask>>()

    extensions.findByType(AndroidComponentsExtension::class.java)?.onVariants { variant ->
        val taskProvider = tasks.register<CollectComposeDependencyAssetsTask>(
            "collect${variant.name.replaceFirstChar { it.uppercase() }}ComposeDependencyAssets"
        ) {
            outputDir.set(layout.buildDirectory.dir("generated/compose/dependencyAssets/${variant.name}"))
        }
        collectTasks += taskProvider
        configureGeneratedDependencyAssets(variant.sources, taskProvider)
    }

    afterEvaluate {
        val runtimeProjectPaths = configurations
            .matching { configuration ->
                configuration.name.endsWith("RuntimeClasspath") && !configuration.name.contains("Test")
            }
            .flatMap { configuration ->
                configuration.incoming.resolutionResult.allComponents.mapNotNull { component ->
                    (component.id as? ProjectComponentIdentifier)?.projectPath
                }
            }
            .distinct()

        if (runtimeProjectPaths.isEmpty()) return@afterEvaluate

        val composeAssetTasks = runtimeProjectPaths.mapNotNull { dependencyPath ->
            val dependencyProject = rootProject.findProject(dependencyPath) ?: return@mapNotNull null
            dependencyProject.tasks.findByName("copyAndroidMainComposeResourcesToAndroidAssets")
                ?.let { dependencyProject.tasks.named("copyAndroidMainComposeResourcesToAndroidAssets") }
        }

        if (composeAssetTasks.isEmpty()) return@afterEvaluate

        val composeAssetOutputs = composeAssetTasks.map { copyTask ->
            copyTask.flatMap { task ->
                val outputDirectoryGetter = task.javaClass.methods.firstOrNull { method ->
                    method.name == "getOutputDirectory" && method.parameterCount == 0
                } ?: error("copyAndroidMainComposeResourcesToAndroidAssets must expose outputDirectory")

                val outputDirectory = outputDirectoryGetter.invoke(task) as? DirectoryProperty
                    ?: error("copyAndroidMainComposeResourcesToAndroidAssets outputDirectory has unexpected type")
                outputDirectory
            }
        }

        collectTasks.forEach { taskProvider ->
            taskProvider.configure {
                assetDirs.from(composeAssetOutputs)
                dependsOn(composeAssetTasks)
            }
        }
    }
}

private fun Project.configureGeneratedDependencyAssets(
    componentSources: Sources,
    taskProvider: org.gradle.api.tasks.TaskProvider<CollectComposeDependencyAssetsTask>,
) {
    componentSources.assets?.addGeneratedSourceDirectory(
        taskProvider,
        CollectComposeDependencyAssetsTask::outputDir,
    )
}
