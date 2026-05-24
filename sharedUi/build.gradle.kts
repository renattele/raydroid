plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
}

kotlin {
    android {
        namespace = "ru.raydroid"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.composeAndroid)
        }
        commonMain.dependencies {
            implementation(libs.bundles.composeCommon)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.bundles.lifecycleCompose)
            implementation(libs.bundles.koin.compose)
            implementation(projects.core.designsystem)
            implementation(projects.feature.search)
            implementation(projects.plugin.api)
            implementation(projects.plugin.host.api)
            implementation(projects.plugin.host.impl)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
