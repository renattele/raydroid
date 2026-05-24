rootProject.name = "Raydroid"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
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
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":sharedUi")
include(":feature:search")
include(":sharedLogic")
include(":androidApp")
include(":desktopApp")
include(":core:data")
include(":core:designsystem")
include(":core:model")
include(":core:domain")
include(":plugin:api")
include(":plugin:host:api")
include(":plugin:host:impl")
include(":plugin:impl:apps")
include(":plugin:impl:calculator")
include(":plugin:impl:files")
include(":plugin:impl:contacts")
include(":plugin:impl:notes")
include(":plugin:impl:weather")
