pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.kikugie.stonecutter") version "0.7.11"
}

rootProject.name = "Tabletop Stuff"

stonecutter {
    create(rootProject) {
        version("1.20.1", "1.20.1").buildscript = "build.forge.gradle.kts"
        version("1.21.1", "1.21.1").buildscript = "build.neoforge.gradle.kts"
        vcsVersion = "1.20.1"
    }
}

// Keep version projects directly in the folders agreed for this repository.
project(":1.20.1").apply {
    projectDir = file("1.20.1")
    buildFileName = "../build.forge.gradle.kts"
}
project(":1.21.1").apply {
    projectDir = file("1.21.1")
    buildFileName = "../build.neoforge.gradle.kts"
}

// Stonecutter initially creates default directories before the remapping above.
// File.delete() removes only empty directories, preserving any existing files.
file("versions/1.20.1").delete()
file("versions/1.21.1").delete()
file("versions").delete()
