plugins {
    alias(libs.plugins.raydroidAndroidLib)
    alias(libs.plugins.raydroidMultiplatform)
}

kotlin {
    android {
        namespace = "ru.raydroid.core.data"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.bundles.koin)
            implementation(libs.bundles.ktorClient)
            implementation(libs.bundles.ktorClient)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
            implementation(projects.core.model)
            implementation(projects.core.domain)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
