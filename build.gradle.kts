// ============================================================================
// J.A.R.V.I.S. TITAN CORE - PROJECT-LEVEL CONFIGURATION 
// ============================================================================
// Architect: DrakoXNaeem
// Description: Master configuration for Android, Kotlin, and Python AI Engine.
// ============================================================================

// 1. BUILDSCRIPT BLOCK: CORE ENGINE INJECTION
// (यह ब्लॉक Gradle को फाॅर्स करेगा कि वो Python इंजन को सही सर्वर से डाउनलोड करे)
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Chaquopy Maven Server for Python Engine
        maven { url = uri("https://chaquo.com/maven") }
    }
    dependencies {
        // 🔥 MASTER FIX: Injecting Python Engine via Classpath instead of Plugin ID
        // इससे आपका "Plugin Not Found" वाला एरर 100% हमेशा के लिए खत्म हो जाएगा।
        classpath("com.chaquo.python:gradle:15.0.0")
    }
}

plugins {
    // 2. Android Application Plugin (Core App Architecture)
    id("com.android.application") version "8.13.2" apply false
    
    // 3. Kotlin Android Plugin (Primary Backend Language)
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    
    // नोट: Chaquopy को ऊपर buildscript में डाल दिया गया है, इसलिए यहाँ id() की जरूरत नहीं है।
}

// ============================================================================
// 4. GLOBAL REPOSITORIES CONFIGURATION
// Ensures all dependencies for Python, Kotlin, and C++ are fetched correctly.
// ============================================================================
allprojects {
    repositories {
        google()
        mavenCentral()
        // Ensuring every module has access to Python packages
        maven { url = uri("https://chaquo.com/maven") }
        // Jitpack for future High-End AI or UI Libraries
        maven { url = uri("https://jitpack.io") }
    }
}

// 5. CACHE CLEAR ENGINE (Optional but good for High-End Apps)
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
