// ============================================================================
// J.A.R.V.I.S. TITAN CORE - PROJECT-LEVEL CONFIGURATION 
// ============================================================================
// Architect: DrakoXNaeem
// Description: Master configuration for Android, Kotlin, and Python AI Engine.
// ============================================================================

plugins {
    // 1. Android Application Plugin (Core App Architecture)
    id("com.android.application") version "8.13.2" apply false
    
    // 2. Kotlin Android Plugin (Primary Backend Language)
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    
    // 3. Chaquopy Python Engine Plugin (AI & Machine Learning Bridge)
    // इसके बिना Python का कोई भी कोड या PIP पैकेज रन नहीं होगा।
    id("com.chaquo.python") version "15.0.0" apply false
}

// ============================================================================
// GLOBAL REPOSITORIES CONFIGURATION
// Ensures all dependencies for Python, Kotlin, and C++ are fetched correctly.
// ============================================================================
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven") }
    }
}
