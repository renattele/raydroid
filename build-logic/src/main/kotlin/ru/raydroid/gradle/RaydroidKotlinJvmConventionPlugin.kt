package ru.raydroid.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val JVM_TOOLCHAIN_VERSION = 21

class RaydroidKotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions
                .findByType<JavaPluginExtension>()
                ?.toolchain
                ?.languageVersion
                ?.set(JavaLanguageVersion.of(JVM_TOOLCHAIN_VERSION))

            extensions
                .findByType<KotlinJvmProjectExtension>()
                ?.jvmToolchain(JVM_TOOLCHAIN_VERSION)

            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
        }
}
