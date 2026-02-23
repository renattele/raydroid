import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.raydroidAndroidApp)
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
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.zipline.loader)
    implementation(libs.okHttp.core)
    implementation(projects.composeApp)
    implementation(projects.plugin.api)
    implementation(projects.plugin.host)
}
