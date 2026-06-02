pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "fraud-prevention-system"

include(":app")
include(":common")
include(":transactions")
include(":rules")
include(":fraud-engine")
include(":case-management")
include(":security")
