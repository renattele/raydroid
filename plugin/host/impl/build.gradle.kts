plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
    alias(libs.plugins.raydroidComposePluginRextResources)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.raydroidZipline)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "ru.raydroid.plugin.host.impl"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
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
            implementation(libs.compose.uiToolingPreview)
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
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
            implementation(projects.plugin.api)
            implementation(projects.plugin.host.api)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fakefilesystem)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
