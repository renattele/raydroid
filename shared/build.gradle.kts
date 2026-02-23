import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.raydroidAndroidLib)
    alias(libs.plugins.raydroidMultiplatform)
}

extensions.configure<KotlinMultiplatformExtension> {
    android {
        namespace = "ru.raydroid"
    }

    js {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
        }
    }
}
