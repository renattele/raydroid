import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    base
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.zipline) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

val detektConfigFile = rootDir.resolve("config/detekt/detekt.yml")
val ktlintVersion = "1.8.0"

configure<KtlintExtension> {
    version.set(ktlintVersion)
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
    config.setFrom(detektConfigFile)
    basePath.set(rootDir)
}

tasks.withType<Detekt>().configureEach {
    exclude("**/build/**")
    exclude("**/generated/**")
}

subprojects {
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        version.set(ktlintVersion)
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

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = false
        parallel = true
        config.setFrom(detektConfigFile)
        basePath.set(rootDir)
    }

    tasks.withType<Detekt>().configureEach {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

val buildableSubprojects = subprojects.filter { it.buildFile.isFile }

tasks.named("check").configure {
    dependsOn(buildableSubprojects.map { "${it.path}:check" })
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

tasks.named("detekt").configure {
    dependsOn(buildableSubprojects.map { "${it.path}:detekt" })
    dependsOn(gradle.includedBuild("build-logic").task(":detekt"))
}

tasks.named("ktlintCheck").configure {
    dependsOn(buildableSubprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(gradle.includedBuild("build-logic").task(":ktlintCheck"))
}

tasks.register("qualityCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests, detekt, and ktlint across the main build and included build logic."
    dependsOn("check", "detekt", "ktlintCheck")
}
