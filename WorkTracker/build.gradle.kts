// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.kotlin.gradle)
        // Dependency Injection
        classpath(libs.di.hilt.gradle)
        // Navigation
        classpath(libs.navigation.args.gradle)
        // Crashlytics
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
