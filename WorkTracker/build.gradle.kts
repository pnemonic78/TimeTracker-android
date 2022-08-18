// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Kotlin.Version.kotlin}")
        // Dependency Injection
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Android.Version.hilt}")
        // Navigation
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Android.Version.navigation}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
