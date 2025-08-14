pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
    plugins {
        id("com.android.application") version "8.6.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.21"
        id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    }
}

include(":app")
rootProject.name = "NB4A"
