// ═══════════════════════════════════════════════════════════════════════════════
// 🤖 J.A.R.V.I.S. TITAN CORE V500.0
// COMPLETE BUILD CONFIGURATION - ALL FEATURES INCLUDED
// ═══════════════════════════════════════════════════════════════════════════════

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
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
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILD TYPES
    // ═══════════════════════════════════════════════════════════════════════════
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JAVA / KOTLIN COMPILATION
    // ═══════════════════════════════════════════════════════════════════════════
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILD FEATURES
    // ═══════════════════════════════════════════════════════════════════════════
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PACKAGING OPTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/INDEX.LIST"
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LINT
    // ═══════════════════════════════════════════════════════════════════════════
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf(
            "MissingTranslation",
            "ExtraTranslation",
            "UnusedResources",
            "GoogleAppIndexingWarning"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST OPTIONS
    // ═══════════════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎨 ANDROIDX CORE UI
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MATERIAL DESIGN
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.google.android.material:material:1.11.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧠 LIFECYCLE & VIEWMODEL (MVVM)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // ⚡ KOTLIN COROUTINES
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ═══════════════════════════════════════════════════════════════════════════
    // 📦 JSON PARSING (Gson - No Plugin Needed)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.google.code.gson:gson:2.10.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🌐 NETWORKING (API Calls)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // 💾 ROOM DATABASE (History, Settings)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔐 DATASTORE (Secure Settings)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎵 MEDIA (ExoPlayer)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-dash:2.19.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🖼️ IMAGE LOADING (Coil)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔔 WORK MANAGER (Background Tasks)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // 📊 LOGGING (Timber)
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔓 PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════
    implementation("com.guolindev.permissionx:permissionx:1.7.1")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🛠️ CORE LIBRARY DESUGARING
    // ═══════════════════════════════════════════════════════════════════════════
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧪 TESTING - UNIT
    // ═══════════════════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🧪 TESTING - INSTRUMENTED
    // ═══════════════════════════════════════════════════════════════════════════
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // ═══════════════════════════════════════════════════════════════════════════
    // 🐛 DEBUG ONLY
    // ═══════════════════════════════════════════════════════════════════════════
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}