plugins {
    alias(libs.plugins.raydroidMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.plugin.api)
        }
    }
}