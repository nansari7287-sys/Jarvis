buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven") }
    }
    dependencies {
        // 🔥 ERROR FIXED: Changed false version to the REAL version 15.0.0
        classpath("com.chaquo.python:gradle:15.0.0")
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
}
