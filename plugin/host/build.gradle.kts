import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.raydroidZipline)
}

extensions.configure<KotlinMultiplatformExtension> {
    android {
        namespace = "ru.raydroid.plugin.host"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.zipline)
            implementation(libs.zipline.loader)
            implementation(libs.ktor.client.core)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.io.okio)
            implementation(libs.kotlinx.atomicfu)
            implementation(projects.plugin.api)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
