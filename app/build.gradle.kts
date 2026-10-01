// ═══════════════════════════════════════════════════════════════════════════════
// 🤖 J.A.R.V.I.S. TITAN CORE V500.0
// APP MODULE BUILD CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.jarvis"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.example.jarvis"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "500.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // ═══ NATIVE ABI FILTERS ═══
        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86_64"
            )
        }

        // ═══ CMAKE NATIVE BUILD ARGS ═══
        externalNativeBuild {
            cmake {
                cppFlags += listOf(
                    "-std=c++17",
                    "-O3",
                    "-ffast-math",
                    "-fexceptions",
                    "-frtti"
                )
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_ARM_NEON=TRUE"
                )
            }
        }
    }

    // ═══ BUILD TYPES ═══
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ═══ JAVA / KOTLIN COMPILATION ═══
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    // ═══ BUILD FEATURES ═══
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // ═══ PACKAGING ═══
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += setOf("**/libc++_shared.so")
        }
    }

    // ═══ CMAKE PATH ═══
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // ═══ LINT ═══
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        disable += setOf(
            "MissingTranslation",
            "ExtraTranslation",
            "UnusedResources"
        )
    }

    // ═══ TEST OPTIONS ═══
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // ═══ SPLITS (APK size kam karne ke liye) ═══
    splits {
        abi {
            isEnable = false
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DEPENDENCIES - CLEAN & WORKING
// ═══════════════════════════════════════════════════════════════════════════════

dependencies {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANDROIDX CORE
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // MATERIAL DESIGN
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.google.android.material:material:1.11.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE & VIEWMODEL
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // COROUTINES
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ═══════════════════════════════════════════════════════════════════════════
    // JSON SERIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // ═══════════════════════════════════════════════════════════════════════════
    // NETWORKING
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // ROOM DATABASE
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // DATASTORE (Secure Settings)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ═══════════════════════════════════════════════════════════════════════════
    // EXOPLAYER (Media Playback)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-dash:2.19.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // IMAGE LOADING
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // WORK MANAGER (Background Tasks)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGGING
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.guolindev.permissionx:permissionx:1.7.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // TENSORFLOW LITE (On-Device AI)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE LIBRARY DESUGARING
    // ═══════════════════════════════════════════════════════════════════════════
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTING - UNIT
    // ═══════════════════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.google.truth:truth:1.1.5")

    // ═══════════════════════════════════════════════════════════════════════════
    // TESTING - INSTRUMENTED
    // ═══════════════════════════════════════════════════════════════════════════
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG ONLY
    // ═══════════════════════════════════════════════════════════════════════════
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