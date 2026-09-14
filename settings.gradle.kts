// ============================================================================
// J.A.R.V.I.S. TITAN CORE - ROOT SETTINGS CONFIGURATION
// ============================================================================
// Architect: DrakoXNaeem
// Description: Manages global repositories for Android, Kotlin, and Python
// ============================================================================

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        
        // [CRITICAL] Chaquopy Maven Repository for Python Plugin
        maven { url = uri("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    // Forces all modules to use these centralized repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // [CRITICAL] Chaquopy Maven Repository for Python Dependencies (PIP)
        maven { url = uri("https://chaquo.com/maven") }
    }
}

// System Name Declaration
rootProject.name = "Jarvis"
include(":app")
