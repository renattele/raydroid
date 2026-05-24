plugins {
    alias(libs.plugins.raydroidAndroidLib)
    alias(libs.plugins.raydroidMultiplatform)
}

val appleTargetsEnabled =
    providers
        .gradleProperty("raydroid.feature.search.appleTargets")
        .orNull
        ?.toBooleanStrictOrNull()
        ?: providers
            .gradleProperty("raydroid.appleTargets.default")
            .orNull
            ?.toBooleanStrictOrNull()
        ?: true

kotlin {
    android {
        namespace = "ru.raydroid.feature.search"
    }

    if (appleTargetsEnabled) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "RaydroidShared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.bundles.koin)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.okio)
            implementation(projects.core.data)
            implementation(projects.plugin.api)
            implementation(projects.plugin.host.api)
            implementation(projects.plugin.host.impl)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fakefilesystem)
        }
    }
}
