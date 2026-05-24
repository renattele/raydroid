package ru.raydroid.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.applyKmpBaseConvention() {
    pluginManager.apply("org.jetbrains.kotlin.multiplatform")

    val libs = libsCatalog()

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("commonTest").configure {
            dependencies {
                implementation(libs.library("kotlin-test"))
            }
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
}

internal fun Project.configureMultiplatformTargetsWhenKmpIsPresent() {
    pluginManager.configureWhenKmpPresent(this) {
        jvm()
        if (appleTargetsEnabled()) {
            iosArm64()
            iosSimulatorArm64()
        }
    }
}

internal fun Project.configureKmpAndroidDefaultsWhenPresent() {
    val libs = libsCatalog()

    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        extensions.configure<KotlinMultiplatformExtension> {
            val nestedExtensions = (this as ExtensionAware).extensions
            nestedExtensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                compileSdk {
                    version = release(libs.versionInt("android-compileSdk"))
                }
                minSdk = libs.versionInt("android-minSdk")
            }
        }
    }
}

private fun PluginManager.configureWhenKmpPresent(
    project: Project,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
    withPlugin("org.jetbrains.kotlin.multiplatform") {
        project.extensions.configure<KotlinMultiplatformExtension>(block)
    }
}

private fun Project.appleTargetsEnabled(): Boolean {
    val projectPath = path.removePrefix(":").replace(':', '.')
    val propertyName = "raydroid.$projectPath.appleTargets"
    return providers.gradleProperty(propertyName)
        .orNull
        ?.toBooleanStrictOrNull()
        ?: true
}
