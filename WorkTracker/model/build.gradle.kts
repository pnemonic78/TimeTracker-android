plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.tikalk.worktracker.model"
    compileSdk = Android.Version.compileSdk

    defaultConfig {
        minSdk = Android.Version.minSdk

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = Java.Version.jvm
        targetCompatibility = Java.Version.jvm
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Android.Version.composeCompiler
    }

    kotlinOptions {
        jvmTarget = Java.Version.jvm.toString()
    }
}

dependencies {
    implementation(project(":core"))

    // Jetpack
    implementation(Android.Jetpack.core)

    // Rx
    implementation(Kotlin.Reactive.coroutinesAndroid)

    // Logging
    implementation(Android.Logging.timber)
    implementation(Android.Logging.crashlytics)

    // Database
    implementation(Android.Database.roomKotlin)
    kapt(Android.Database.roomCompiler)

    // Testing
    testImplementation(Android.Test.junit)
    androidTestImplementation(Android.Test.junit_ext)
    androidTestImplementation(Android.Test.espresso_core)
}