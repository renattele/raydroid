import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `kotlin-dsl`
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
}
