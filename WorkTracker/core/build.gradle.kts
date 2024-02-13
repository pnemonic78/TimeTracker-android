plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.tikalk.core"
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

    buildFeatures {
        compose = true
        viewBinding = true
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
    // Jetpack
    implementation(Android.Jetpack.appcompat)
    implementation(Android.Jetpack.composeActivity)
    implementation(Android.Jetpack.composeCompiler)
    implementation(Android.Jetpack.composeIcons)
    implementation(Android.Jetpack.composeMaterial3)
    implementation(Android.Jetpack.composeRuntime)
    implementation(Android.Jetpack.composeUi)
    implementation(Android.Jetpack.composeUiTooling)
    implementation(Android.Jetpack.composeUiToolingPreview)
    implementation(Android.Jetpack.constraintLayout)
    implementation(Android.Jetpack.core)
    implementation(Android.Jetpack.preference)

    // Rx
    implementation(Kotlin.Reactive.coroutinesAndroid)

    // Web
    implementation(Android.Network.logging)
    implementation(Android.Network.okhttp)
    implementation(Android.Network.okhttp_url)
    implementation(Android.Network.retrofit)
    implementation(Android.Network.retrofit_scalars)
    implementation(Java.Network.jsoup)

    // Logging
    implementation(Android.Logging.timber)
    implementation(Android.Logging.crashlytics)

    // Navigation
    implementation(Android.Jetpack.navigationCompose)
    implementation(Android.Jetpack.navigationFragment)
    implementation(Android.Jetpack.navigationUI)

    // Testing
    testImplementation(Android.Test.junit)
    androidTestImplementation(Android.Test.junit_ext)
    androidTestImplementation(Android.Test.espresso_core)
}