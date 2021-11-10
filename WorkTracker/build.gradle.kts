// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildVersions.kotlin_version}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${BuildVersions.nav_version}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
