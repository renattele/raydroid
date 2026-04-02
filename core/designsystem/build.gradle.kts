plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
}

kotlin {
    android {
        namespace = "ru.raydroid.core.designsystem"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.composeCommon)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
