package ru.raydroid.gradle

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class RaydroidAndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = libsCatalog()

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.versionInt("android-compileSdk")

            defaultConfig {
                minSdk = libs.versionInt("android-minSdk")
                targetSdk = libs.versionInt("android-targetSdk")
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    }
}
