// ═══════════════════════════════════════════════════════════════════════════════
// 🤖 JARVIS AI - BUILD.GRADLE
// Professional Build Configuration with Future-Ready Features
// ═══════════════════════════════════════════════════════════════════════════════

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")                                    // Room database processor
    id("kotlin-parcelize")                               // Parcelable support
    id("kotlinx-serialization")                          // JSON serialization
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" // KSP for Room
}

android {
    namespace = "com.example.jarvis"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.jarvis"
        minSdk = 26          // Android 8.0+ (for better APIs)
        targetSdk = 34       // Android 14
        versionCode = 1
        versionName = "1.0.0"
        
        // ═══════════════════════════════════════════════════════════
        // TEST RUNNER
        // ═══════════════════════════════════════════════════════════
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ═══════════════════════════════════════════════════════════
        // VECTOR DRAWABLE SUPPORT
        // ═══════════════════════════════════════════════════════════
        vectorDrawables {
            useSupportLibrary = true
        }

        // ═══════════════════════════════════════════════════════════
        // NDK CONFIGURATION (C++ Native Support)
        // ═══════════════════════════════════════════════════════════
        ndk {
            abiFilters += listOf(
                "armeabi-v7a",   // 32-bit ARM
                "arm64-v8a",     // 64-bit ARM (most modern phones)
                "x86",           // 32-bit Intel (emulator)
                "x86_64"         // 64-bit Intel (emulator)
            )
        }

        // ═══════════════════════════════════════════════════════════
        // CMAKE ARGUMENTS FOR C++ COMPILATION
        // ═══════════════════════════════════════════════════════════
        externalNativeBuild {
            cmake {
                cppFlags += listOf(
                    "-std=c++17",          // Modern C++
                    "-O3",                 // Max optimization
                    "-ffast-math",         // Fast math operations
                    "-fexceptions",        // Exception support
                    "-frtti"               // RTTI support
                )
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_ARM_NEON=TRUE"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILD VARIANTS (Debug / Release)
    // ═══════════════════════════════════════════════════════════════
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            // Debug ke liye fake API keys ya test server
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release signing config (baad me setup karenge)
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // JAVA / KOTLIN COMPILATION OPTIONS
    // ═══════════════════════════════════════════════════════════════
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true  // Java 8+ APIs on old Android
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILD FEATURES
    // ═══════════════════════════════════════════════════════════════
    buildFeatures {
        viewBinding = true
        buildConfig = true          // BuildConfig fields generate karne ke liye
        compose = false             // Traditional XML use kar rahe hain
    }

    // ═══════════════════════════════════════════════════════════════
    // PACKAGING OPTIONS
    // ═══════════════════════════════════════════════════════════════
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += setOf("**/libc++_shared.so")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CMAKE BUILD PATH
    // ═══════════════════════════════════════════════════════════════
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LINT OPTIONS
    // ═══════════════════════════════════════════════════════════════
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        disable += setOf(
            "MissingTranslation",
            "ExtraTranslation"
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST OPTIONS
    // ═══════════════════════════════════════════════════════════════
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DEPENDENCIES - COMPLETE STACK
// ═══════════════════════════════════════════════════════════════════════════════

dependencies {
    
    // ═══════════════════════════════════════════════════════════════
    // 🎨 ANDROIDX CORE
    // ═══════════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 🎯 MATERIAL DESIGN
    // ═══════════════════════════════════════════════════════════════
    implementation("com.google.android.material:material:1.11.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 🧠 LIFECYCLE & VIEWMODEL (MVVM Architecture)
    // ═══════════════════════════════════════════════════════════════
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    
    // ═══════════════════════════════════════════════════════════════
    // ⚡ KOTLIN COROUTINES (Async Operations)
    // ═══════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // ═══════════════════════════════════════════════════════════════
    // 📦 JSON SERIALIZATION (AI Response Parsing)
    // ═══════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // ═══════════════════════════════════════════════════════════════
    // 🌐 NETWORKING (AI API Calls)
    // ═══════════════════════════════════════════════════════════════
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.9.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 💾 DATABASE - ROOM (Command History, Settings)
    // ═══════════════════════════════════════════════════════════════
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // ═══════════════════════════════════════════════════════════════
    // 🔐 DATASTORE (Secure Settings Storage)
    // ═══════════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // ═══════════════════════════════════════════════════════════════
    // 🎵 MEDIA - EXOPLAYER (Video/Audio Playback)
    // ═══════════════════════════════════════════════════════════════
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-dash:2.19.1")
    
    // ═══════════════════════════════════════════════════════════════
    // 🖼️ IMAGE LOADING (Screenshots, Icons)
    // ═══════════════════════════════════════════════════════════════
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 🔔 WORK MANAGER (Background Tasks)
    // ═══════════════════════════════════════════════════════════════
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 📊 LOGGING (Debugging)
    // ═══════════════════════════════════════════════════════════════
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // ═══════════════════════════════════════════════════════════════
    // 🎯 PERMISSIONS (Easy Runtime Permissions)
    // ═══════════════════════════════════════════════════════════════
    implementation("com.guolindev.permissionx:permissionx:1.7.1")
    
    // ═══════════════════════════════════════════════════════════════
    // 🚀 TENSORFLOW LITE (On-Device AI)
    // ═══════════════════════════════════════════════════════════════
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    
    // ═══════════════════════════════════════════════════════════════
    // 🎙️ SPEECH RECOGNITION (Enhanced)
    // ═══════════════════════════════════════════════════════════════
    implementation("com.github.gotev:speech:1.6.4")  // Easy speech recognition
    
    // ═══════════════════════════════════════════════════════════════
    // 📱 ACCESSIBILITY ENHANCEMENTS
    // ═══════════════════════════════════════════════════════════════
    implementation("com.github.aakira:napier:2.7.1")  // Multi-platform logging
    
    // ═══════════════════════════════════════════════════════════════
    // 🛠️ CORE LIBRARY DESUGARING (Backward Compatibility)
    // ═══════════════════════════════════════════════════════════════
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // ═══════════════════════════════════════════════════════════════
    // 🧪 TESTING - UNIT TESTS
    // ═══════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")
    
    // ═══════════════════════════════════════════════════════════════
    // 🧪 TESTING - INSTRUMENTED TESTS
    // ═══════════════════════════════════════════════════════════════
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // ═══════════════════════════════════════════════════════════════
    // 🐛 DEBUG ONLY DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
}

// ═══════════════════════════════════════════════════════════════════════════════
// KSP CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// ═══════════════════════════════════════════════════════════════════════════════
// CLEAN TASK
// ═══════════════════════════════════════════════════════════════════════════════
tasks.register<Delete>("cleanAll") {
    delete(rootProject.layout.buildDirectory)
    delete("$projectDir/.cxx")
}