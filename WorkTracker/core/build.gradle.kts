plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.tikalk.core"
    compileSdk = Android.Version.compileSdk

    defaultConfig {
        minSdk = Android.Version.minSdk
        targetSdk = Android.Version.targetSdk

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Android.Version.composeCompiler
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    // Jetpack
    implementation(Android.Jetpack.appcompat)
    implementation(Android.Jetpack.composeActivity)
    implementation(Android.Jetpack.composeCompiler)
    implementation(Android.Jetpack.composeIcons)
    implementation(Android.Jetpack.composeMaterial)
    implementation(Android.Jetpack.composeRuntime)
    implementation(Android.Jetpack.composeUi)
    implementation(Android.Jetpack.composeUiTooling)
    implementation(Android.Jetpack.composeUiToolingPreview)
    implementation(Android.Jetpack.constraint_layout)
    implementation(Android.Jetpack.core)
    implementation(Android.Jetpack.preference)
    implementation(Android.Jetpack.security)

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
    implementation(Android.Navigation.navigation_fragment)
    implementation(Android.Navigation.navigation_ui)

    // Testing
    testImplementation(Android.Test.junit)
    androidTestImplementation(Android.Test.junit_ext)
    androidTestImplementation(Android.Test.espresso_core)
}