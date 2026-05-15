import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.raydroidComposeMultiplatform)
    alias(libs.plugins.raydroidComposePluginRextResources)
    alias(libs.plugins.raydroidMultiplatform)
    alias(libs.plugins.raydroidZipline)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val outlinedMaterialIconSources: Configuration by configurations.creating
val generatedOutlinedIconsDir: Provider<Directory> = layout.buildDirectory.dir("generated/source/outlinedMaterialIcons/commonMain/kotlin")
val generatedOutlinedIconsFile: Provider<RegularFile> = generatedOutlinedIconsDir.map {
    it.file("ru/raydroid/plugin/host/impl/presentation/generated/GeneratedOutlinedMaterialIconRegistry.kt")
}

abstract class GenerateOutlinedMaterialIconRegistryTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceJars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val iconNames = sourceJars.files
            .asSequence()
            .flatMap { sourceJar ->
                ZipFile(sourceJar).use { zipFile ->
                    zipFile.entries().asSequence()
                        .map { it.name }
                        .filter { path ->
                            path.startsWith("commonMain/androidx/compose/material/icons/outlined/")
                                && path.endsWith(".kt")
                        }
                        .map { path -> path.substringAfterLast('/').removeSuffix(".kt") }
                        .filterNot { iconName -> iconName == "Addchart" }
                        .toList()
                }.asSequence()
            }
            .distinct()
            .sorted()
            .toList()
        val iconChunks = iconNames.chunked(200)

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                appendLine("package ru.raydroid.plugin.host.impl.presentation.generated")
                appendLine()
                appendLine("import androidx.compose.material.icons.Icons")
                appendLine("import androidx.compose.material.icons.outlined.*")
                appendLine("import androidx.compose.ui.graphics.vector.ImageVector")
                appendLine()
                appendLine("internal object GeneratedOutlinedMaterialIconRegistry {")
                appendLine("    fun resolve(name: String): ImageVector? {")
                if (iconChunks.isEmpty()) {
                    appendLine("        return null")
                } else {
                    append("        return ")
                    appendLine(
                        iconChunks.indices.joinToString(" ?: ") { index ->
                            "resolveChunk$index(name)"
                        }
                    )
                }
                appendLine("    }")
                iconChunks.forEachIndexed { index, chunk ->
                    appendLine()
                    appendLine("    private fun resolveChunk$index(name: String): ImageVector? = when (name) {")
                    chunk.forEach { iconName ->
                        appendLine("        \"$iconName\" -> Icons.Outlined.$iconName")
                    }
                    appendLine("        else -> null")
                    appendLine("    }")
                }
                appendLine("}")
            }
        )
    }
}

val generateOutlinedMaterialIconRegistry = tasks.register<GenerateOutlinedMaterialIconRegistryTask>(
    "generateOutlinedMaterialIconRegistry"
) {
    sourceJars.from(outlinedMaterialIconSources)
    outputFile.set(generatedOutlinedIconsFile)
}

kotlin {
    android {
        namespace = "ru.raydroid.plugin.host.impl"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(generatedOutlinedIconsDir)
            dependencies {
                implementation(libs.bundles.composeCommon)
                implementation(libs.compose.icons.extended)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.bundles.lifecycleCompose)
                implementation(libs.bundles.ziplineRuntime)
                implementation(libs.ktor.client.core)
                implementation(libs.bundles.dataStore)
                implementation(libs.bundles.kotlinxIo)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.okio)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.koin.core)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)
                implementation(projects.core.designsystem)
                implementation(projects.plugin.api)
                implementation(projects.plugin.host.api)
            }
        }
        androidMain.dependencies {
            implementation(libs.bundles.composeAndroid)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.okio.fakefilesystem)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    outlinedMaterialIconSources("org.jetbrains.compose.material:material-icons-core:${libs.versions.composeIcons.get()}:sources@jar")
    outlinedMaterialIconSources("org.jetbrains.compose.material:material-icons-extended:${libs.versions.composeIcons.get()}:sources@jar")
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

tasks.matching { task ->
    task.name.startsWith("compileKotlin") || task.name.startsWith("ksp")
}.configureEach {
    dependsOn(generateOutlinedMaterialIconRegistry)
}

room {
    schemaDirectory("$projectDir/schemas")
}
