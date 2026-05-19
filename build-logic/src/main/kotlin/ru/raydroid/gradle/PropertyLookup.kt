package ru.raydroid.gradle

import org.gradle.api.Project

internal fun Project.findEnvironmentGradleOrLocalProperty(
    gradlePropertyName: String,
    environmentName: String,
): String? =
    providers.environmentVariable(environmentName).orNull
        ?: providers.gradleProperty(gradlePropertyName).orNull
        ?: findLocalProperty(gradlePropertyName)

private fun Project.findLocalProperty(name: String): String? {
    val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties").asFile
    if (!localPropertiesFile.isFile) return null

    return localPropertiesFile.useLines { lines ->
        lines
            .map { it.trim() }
            .firstOrNull { line -> line.startsWith("$name=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
