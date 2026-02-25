import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
    alias(libs.plugins.raydroidComposePluginRextResources)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.raydroidZipline)
}

extensions.configure<KotlinMultiplatformExtension> {
    android {
        namespace = "ru.raydroid.plugin.host"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.composeAndroid)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.bundles.composeCommon)
            implementation(libs.bundles.lifecycleCompose)
            implementation(libs.bundles.ziplineRuntime)
            implementation(libs.ktor.client.core)
            implementation(libs.bundles.dataStore)
            implementation(libs.bundles.kotlinxIo)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.okio)
            implementation(libs.okio.fakefilesystem)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
            implementation(projects.plugin.api)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
