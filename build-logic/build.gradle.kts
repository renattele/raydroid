import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

group = "ru.raydroid.buildlogic"

repositories {
    google {
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins {
        register("raydroidAndroidApp") {
            id = "raydroid.android.app"
            implementationClass = "ru.raydroid.gradle.RaydroidAndroidApplicationConventionPlugin"
        }
        register("raydroidAndroidLib") {
            id = "raydroid.android.lib"
            implementationClass = "ru.raydroid.gradle.RaydroidKmpAndroidLibraryConventionPlugin"
        }
        register("raydroidMultiplatform") {
            id = "raydroid.multiplatform"
            implementationClass = "ru.raydroid.gradle.RaydroidMultiplatformConventionPlugin"
        }
        register("raydroidComposeMultiplatform") {
            id = "raydroid.compose.multiplatform"
            implementationClass = "ru.raydroid.gradle.RaydroidKmpComposeAndroidLibraryConventionPlugin"
        }
        register("raydroidComposePluginRextResources") {
            id = "raydroid.compose.plugin.rext.resources"
            implementationClass = "ru.raydroid.gradle.RaydroidComposePluginRextResourcesConventionPlugin"
        }
        register("raydroidZipline") {
            id = "raydroid.zipline"
            implementationClass = "ru.raydroid.gradle.RaydroidKmpZiplineConventionPlugin"
        }
        register("raydroidPlugin") {
            id = "raydroid.plugin"
            implementationClass = "ru.raydroid.gradle.RaydroidPluginConventionPlugin"
        }
        register("raydroidJvm") {
            id = "raydroid.jvm"
            implementationClass = "ru.raydroid.gradle.RaydroidKotlinJvmConventionPlugin"
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    compileOnly("com.android.tools.build:gradle-api:${libs.versions.agp.get()}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:${libs.versions.composeMultiplatform.get()}")
}

configure<KtlintExtension> {
    version.set("1.8.0")
    enableExperimentalRules.set(true)
    outputToConsole.set(true)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
    filter {
        include("**/*.kt")
        include("**/*.kts")
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = false
    parallel = true
    config.setFrom(rootDir.resolve("../config/detekt/detekt.yml"))
    basePath.set(rootDir.resolve(".."))
}

tasks.withType<Detekt>().configureEach {
    exclude("**/build/**")
    exclude("**/generated/**")
}
