plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
}

kotlin {
    android {
        namespace = "ru.raydroid"
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.composeAndroid)
        }
        commonMain.dependencies {
            implementation(libs.bundles.composeCommon)
            implementation(libs.bundles.lifecycleCompose)
            implementation(projects.shared)
            implementation(projects.plugin.api)
            implementation(projects.plugin.host)
        }
    }
}
