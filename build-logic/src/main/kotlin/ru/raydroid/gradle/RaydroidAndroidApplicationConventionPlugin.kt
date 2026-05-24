package ru.raydroid.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Sources
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

abstract class CollectComposeDependencyAssetsTask
    @Inject
    constructor(
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
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val libs = libsCatalog()
            val releaseSigning = releaseSigningSpec()

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

                signingConfigs {
                    create("release") {
                        storeFile = releaseSigning.storeFile
                        storePassword = releaseSigning.storePassword
                        keyAlias = releaseSigning.keyAlias
                        keyPassword = releaseSigning.keyPassword
                    }
                }

                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                        signingConfig = signingConfigs.getByName("release")
                        if (releaseSigning.usesDefaultTestSigning) {
                            optimization {
                                baselineProfile {
                                    ignoreFromAllExternalDependencies = true
                                }
                            }
                        }
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

private data class ReleaseSigningSpec(
    val storeFile: java.io.File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
    val usesDefaultTestSigning: Boolean,
)

private fun Project.releaseSigningSpec(): ReleaseSigningSpec {
    val defaultStoreFile = layout.projectDirectory.file("signing/test-release.keystore").asFile
    val defaultStorePassword = "raydroid-test-release"
    val defaultKeyAlias = "raydroidTestRelease"
    val defaultKeyPassword = "raydroid-test-release"

    val storeFileOverride =
        findEnvironmentGradleOrLocalProperty(
            gradlePropertyName = "raydroid.android.release.storeFile",
            environmentName = "RAYDROID_ANDROID_RELEASE_STORE_FILE",
        )
    val storePasswordOverride =
        findEnvironmentGradleOrLocalProperty(
            gradlePropertyName = "raydroid.android.release.storePassword",
            environmentName = "RAYDROID_ANDROID_RELEASE_STORE_PASSWORD",
        )
    val keyAliasOverride =
        findEnvironmentGradleOrLocalProperty(
            gradlePropertyName = "raydroid.android.release.keyAlias",
            environmentName = "RAYDROID_ANDROID_RELEASE_KEY_ALIAS",
        )
    val keyPasswordOverride =
        findEnvironmentGradleOrLocalProperty(
            gradlePropertyName = "raydroid.android.release.keyPassword",
            environmentName = "RAYDROID_ANDROID_RELEASE_KEY_PASSWORD",
        )

    val overrides =
        listOf(
            "raydroid.android.release.storeFile" to storeFileOverride,
            "raydroid.android.release.storePassword" to storePasswordOverride,
            "raydroid.android.release.keyAlias" to keyAliasOverride,
            "raydroid.android.release.keyPassword" to keyPasswordOverride,
        )
    val providedOverrideCount = overrides.count { (_, value) -> !value.isNullOrBlank() }
    if (providedOverrideCount != 0 && providedOverrideCount != overrides.size) {
        val missingOverrides =
            overrides
                .filter { (_, value) -> value.isNullOrBlank() }
                .joinToString { (name, _) -> name }
        throw GradleException(
            "Release signing override incomplete. Set all signing values or none. Missing: $missingOverrides",
        )
    }

    return ReleaseSigningSpec(
        storeFile = rootProject.file(storeFileOverride ?: defaultStoreFile.absolutePath),
        storePassword = storePasswordOverride ?: defaultStorePassword,
        keyAlias = keyAliasOverride ?: defaultKeyAlias,
        keyPassword = keyPasswordOverride ?: defaultKeyPassword,
        usesDefaultTestSigning = providedOverrideCount == 0,
    )
}

private fun Project.configureAndroidComposeDependencyAssets() {
    val collectTasks = mutableListOf<org.gradle.api.tasks.TaskProvider<CollectComposeDependencyAssetsTask>>()

    extensions.findByType(AndroidComponentsExtension::class.java)?.onVariants { variant ->
        val taskProvider =
            tasks.register<CollectComposeDependencyAssetsTask>(
                "collect${variant.name.replaceFirstChar { it.uppercase() }}ComposeDependencyAssets",
            ) {
                outputDir.set(layout.buildDirectory.dir("generated/compose/dependencyAssets/${variant.name}"))
            }
        collectTasks += taskProvider
        configureGeneratedDependencyAssets(variant.sources, taskProvider)
    }

    afterEvaluate {
        val runtimeProjectPaths =
            configurations
                .matching { configuration ->
                    configuration.name.endsWith("RuntimeClasspath") && !configuration.name.contains("Test")
                }.flatMap { configuration ->
                    configuration.incoming.resolutionResult.allComponents.mapNotNull { component ->
                        (component.id as? ProjectComponentIdentifier)?.projectPath
                    }
                }.distinct()

        if (runtimeProjectPaths.isEmpty()) return@afterEvaluate

        val composeAssetTasks =
            runtimeProjectPaths.mapNotNull { dependencyPath ->
                val dependencyProject = rootProject.findProject(dependencyPath) ?: return@mapNotNull null
                dependencyProject.tasks
                    .findByName("copyAndroidMainComposeResourcesToAndroidAssets")
                    ?.let { dependencyProject.tasks.named("copyAndroidMainComposeResourcesToAndroidAssets") }
            }

        if (composeAssetTasks.isEmpty()) return@afterEvaluate

        val composeAssetOutputs =
            composeAssetTasks.map { copyTask ->
                copyTask.flatMap { task ->
                    val outputDirectoryGetter =
                        task.javaClass.methods.firstOrNull { method ->
                            method.name == "getOutputDirectory" && method.parameterCount == 0
                        } ?: error("copyAndroidMainComposeResourcesToAndroidAssets must expose outputDirectory")

                    val outputDirectory =
                        outputDirectoryGetter.invoke(task) as? DirectoryProperty
                            ?: error(
                                "copyAndroidMainComposeResourcesToAndroidAssets outputDirectory has unexpected type",
                            )
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
