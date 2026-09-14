// ============================================================================
// J.A.R.V.I.S. TITAN CORE - SETTINGS CONFIGURATION
// ============================================================================

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    // 🔥 CRITICAL FIX: Changed to PREFER_PROJECT to allow buildscript repositories
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven") }
    }
}

rootProject.name = "Jarvis"
include(":app")
