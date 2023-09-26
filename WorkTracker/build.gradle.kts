// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Kotlin.Version.kotlin}")
        // Dependency Injection
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Android.Version.hilt}")
        // Navigation
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Android.Version.navigation}")
        // Crashlytics
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
