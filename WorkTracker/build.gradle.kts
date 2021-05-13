// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildVersions.kotlin_version}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${BuildVersions.nav_version}")
    }
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}
