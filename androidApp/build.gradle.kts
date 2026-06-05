import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.raydroidAndroidApp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

extensions.configure<ApplicationExtension> {
    namespace = "ru.raydroid"

    defaultConfig {
        applicationId = "ru.raydroid"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.bundles.composeAndroid)
    implementation(libs.zipline.loader)
    implementation(libs.okHttp.core)
    implementation(libs.koin.android)
    implementation(projects.sharedUi)
    implementation(projects.plugin.api)
    implementation(projects.plugin.host.api)
    implementation(projects.plugin.host.impl)
    testImplementation(libs.junit)
}
