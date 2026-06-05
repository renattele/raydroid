import app.cash.zipline.gradle.ZiplineExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    alias(libs.plugins.raydroidPlugin)
    alias(libs.plugins.serialization)
}

extensions.configure<KotlinMultiplatformExtension> {
    js {
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.zipline)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.plugin.api)
        }
    }
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().yarnLockAutoReplace = true
}

extensions.configure<ZiplineExtension> {
    mainFunction = "main"
    optimizeForSmallArtifactSize()
}
