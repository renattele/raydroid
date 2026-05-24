plugins {
    alias(libs.plugins.raydroidMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.okio)
        }
    }
}
