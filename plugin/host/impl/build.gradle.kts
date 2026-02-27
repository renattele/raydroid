plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
    alias(libs.plugins.raydroidComposePluginRextResources)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.raydroidZipline)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "ru.raydroid.plugin.host.impl"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.composeAndroid)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
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
            implementation(libs.koin.core)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
            implementation(projects.plugin.api)
            implementation(projects.plugin.host.api)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
