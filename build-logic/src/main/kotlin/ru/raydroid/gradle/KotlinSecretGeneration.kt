package ru.raydroid.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

fun Project.generateKotlinSecret(
    packageName: String,
    constantName: String,
    propertyName: String,
    environmentName: String,
    sourceSetName: String = "jsMain",
): GenerateKotlinSecret {
    val taskName = "generate${constantName.replaceFirstChar { it.uppercaseChar() }}Secret"
    val outputDir = layout.buildDirectory.dir("generated/secrets/$sourceSetName/kotlin")
    val secret = providers.provider {
        findEnvironmentGradleOrLocalProperty(
            gradlePropertyName = propertyName,
            environmentName = environmentName,
        ).orEmpty()
    }

    val task = tasks.register<GenerateKotlinSecretTask>(taskName) {
        this.packageName.set(packageName)
        this.constantName.set(constantName)
        this.secret.set(secret)
        this.outputDir.set(outputDir)
    }

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named(sourceSetName) {
            kotlin.srcDir(task.flatMap { it.outputDir })
        }
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        dependsOn(task)
    }

    return GenerateKotlinSecret(task)
}

class GenerateKotlinSecret internal constructor(
    val task: org.gradle.api.tasks.TaskProvider<GenerateKotlinSecretTask>,
)

abstract class GenerateKotlinSecretTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val constantName: Property<String>

    @get:Input
    abstract val secret: Property<String>

    @get:OutputDirectory
    abstract val outputDir: org.gradle.api.file.DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDir.get()
            .file("${packageName.get().replace('.', '/')}/${constantName.get()}.kt")
            .asFile

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package ${packageName.get()}

            internal const val ${constantName.get()} = "${secret.get().escapeKotlinString()}"
            """.trimIndent()
        )
    }
}

private fun String.escapeKotlinString(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
