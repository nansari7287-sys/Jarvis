// ============================================================================
// J.A.R.V.I.S. TITAN CORE - PROJECT-LEVEL CONFIGURATION
// ============================================================================

// 🔥 MASTER FIX: Using explicit classpath bypasses the "Plugin not found" error completely.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven") }
    }
    dependencies {
        // Direct link to the latest stable Chaquopy engine
        classpath("com.chaquo.python:gradle:15.6.2")
    }
}

plugins {
    // 1. Android Application Plugin
    id("com.android.application") version "8.13.2" apply false
    
    // 2. Kotlin Android Plugin
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    
    // NOTE: Chaquopy is now handled by the buildscript block above.
}
