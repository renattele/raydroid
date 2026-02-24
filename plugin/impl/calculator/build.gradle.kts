import app.cash.zipline.gradle.ZiplineExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.raydroidZipline)
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
