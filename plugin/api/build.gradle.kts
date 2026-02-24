import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.raydroidZipline)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.serialization)
}

extensions.configure<KotlinMultiplatformExtension> {
    js {
        browser()
        binaries.executable()
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add("-Xir-minimized-member-names=false")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.zipline)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
