plugins {
    aliasId(libs.plugins.androidLibrary)
    aliasId(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
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

    kotlinOptions {
        jvmTarget = Java.Version.jvm.toString()
    }
}

dependencies {
    // Jetpack
    implementation(libs.jetpack.appcompat)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.jetpack)

    // Rx
    implementation(libs.coroutines.android)

    // Web
    implementation(libs.bundles.net)
    implementation(libs.html.jsoup)

    // Logging
    implementation(libs.bundles.logging)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Testing
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}