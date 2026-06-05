import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.serialization)
}

group = "ru.raydroid"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.bundles.composeCommon)
    implementation(libs.bundles.ziplineRuntime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.koin.compose)
    implementation(compose.desktop.currentOs)
    implementation(projects.sharedUi)
    implementation(projects.feature.search)
    implementation(projects.plugin.api)
    implementation(projects.plugin.host.api)
    implementation(projects.plugin.host.impl)
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
            packageName = "Raydroid"
            packageVersion = "1.0.0"
            modules(
                "java.base",
                "java.desktop",
                "java.instrument",
                "java.logging",
                "java.management",
                "java.net.http",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.unsupported",
            )
            macOS {
                iconFile.set(project.file("src/main/resources/Raydroid.icns"))
            }
        }
    }
}
