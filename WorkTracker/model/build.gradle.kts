plugins {
    aliasId(libs.plugins.androidLibrary)
    aliasId(libs.plugins.kapt)
    aliasId(libs.plugins.kotlinAndroid)
    aliasId(libs.plugins.kotlinParcelize)
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

    kotlinOptions {
        jvmTarget = Java.Version.jvm.toString()
    }
}

dependencies {
    implementation(projects.core)

    // Jetpack
    implementation(libs.jetpack.core)

    // Logging
    implementation(libs.bundles.logging)

    // Database
    implementation(libs.db.room.kotlin)
    kapt(libs.db.room.compiler)

    // Testing
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}