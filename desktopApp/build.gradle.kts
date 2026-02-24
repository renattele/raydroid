import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

group = "ru.raydroid"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.bundles.composeCommon)
    implementation(libs.bundles.ziplineRuntime)
    implementation(compose.desktop.currentOs)
    implementation(projects.composeApp)
    implementation(projects.plugin.api)
    implementation(projects.plugin.host)
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

compose.desktop {
    application {
        mainClass = "ru.raydroid.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RayDroid"
            packageVersion = "1.0.0"
        }
    }
}