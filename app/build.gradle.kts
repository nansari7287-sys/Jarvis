// ============================================================================
// J.A.R.V.I.S. TITAN CORE - MODULE-LEVEL CONFIGURATION (V52.0)
// ============================================================================
// Architect: DrakoXNaeem
// Contains: Kotlin UI, Python AI Runtime (Chaquopy), and C++ Matrix Engine (CMake)
// ============================================================================

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 1. Python Engine Plugin Initialized
    id("com.chaquo.python")
}

android {
    namespace = "com.example.jarvis"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.jarvis"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // 2. NDK ABI Filters (Required for C++ and Python native binaries)
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        // 3. C++ Native CMake Configuration (Extreme Performance Flags)
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3", "-ffast-math")
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 4. Linking the C++ CMakeLists.txt Path
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Resolves packaging conflicts between Python and Android binaries
    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

// ============================================================================
// 5. PYTHON AI ENGINE & PIP PACKAGE MANAGER
// ============================================================================
chaquopy {
    defaultConfig {
        // Core Python Version
        version = "3.11"
        
        // Exposing built-in build properties
        buildPython("python3")

        // PIP: Complete Neural & Scientific Modules Installed Locally
        pip {
            install("numpy")                 // High-speed matrix/math calculations
            install("scipy")                 // Advanced scientific computing
            install("pandas")                // Data structure handling
            install("requests")              // Web/API request handling
            install("openai")                // OpenAI API Bridge for offline/online GPT
            install("google-generativeai")   // Gemini Pro API Bridge
            install("SpeechRecognition")     // Offline Python audio processing fallback
        }
    }
}

// ============================================================================
// 6. KOTLIN DEPENDENCIES
// ============================================================================
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // LiteRT / TensorFlow Lite runtime for the Hey Jarvis model
    implementation("com.google.ai.edge.litert:litert:2.1.0")
}
