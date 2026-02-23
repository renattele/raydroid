plugins {
    alias(libs.plugins.raydroidJvm)
    alias(libs.plugins.ktor)
    application
}

group = "ru.raydroid"
version = "1.0.0"
application {
    mainClass.set("ru.raydroid.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.bundles.ktorServer)
    testImplementation(libs.bundles.serverTest)
}
