pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Chaquopy Plugin Server
        maven { url = uri("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Chaquopy Dependencies (PIP) Server
        maven { url = uri("https://chaquo.com/maven") }
    }
}

rootProject.name = "Jarvis"
include(":app")
