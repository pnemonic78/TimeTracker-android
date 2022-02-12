plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

val versionMajor = (project.properties["APP_VERSION_MAJOR"] as String).toInt()
val versionMinor = (project.properties["APP_VERSION_MINOR"] as String).toInt()

android {
    compileSdk = BuildVersions.compileSdkVersion

    defaultConfig {
        applicationId = "com.tikalk.worktracker"
        minSdk = BuildVersions.minSdkVersion
        targetSdk = BuildVersions.targetSdkVersion
        versionCode = versionMajor * 1000 + versionMinor
        versionName = "${versionMajor}." + versionMinor.toString().padStart(2, '0')

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = project.properties["STORE_PASSWORD_RELEASE"] as String
            keyAlias = "release"
            keyPassword = project.properties["KEY_PASSWORD_RELEASE"] as String
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            // disabled until fix proguard issues: minifyEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        getByName("androidTest") {
            java { srcDir(file("src/androidTest/kotlin")) }
        }
        getByName("main") {
            java { srcDir(file("src/main/kotlin")) }
        }
        getByName("test") {
            java { srcDir(file("src/test/kotlin")) }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    packagingOptions {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Jetpack
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.6.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha03")

    // Testing
    androidTestImplementation("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    testImplementation("junit:junit:${BuildVersions.junitVersion}")

    // Database
    implementation("androidx.room:room-common:${BuildVersions.roomVersion}")
    implementation("androidx.room:room-runtime:${BuildVersions.roomVersion}")
    implementation("androidx.room:room-rxjava3:${BuildVersions.roomVersion}")
    kapt("androidx.room:room-compiler:${BuildVersions.roomVersion}")

    // Rx
    implementation("com.squareup.retrofit2:adapter-rxjava3:${BuildVersions.retrofit2Version}")
    implementation("io.reactivex.rxjava3:rxandroid:${BuildVersions.rxAndroidVersion}")
    implementation("io.reactivex.rxjava3:rxkotlin:${BuildVersions.rxKotlinVersion}")

    // Web
    implementation("com.squareup.retrofit2:retrofit:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.retrofit2:converter-scalars:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.okhttp3:logging-interceptor:${BuildVersions.okhttpVersion}")
    implementation("com.squareup.okhttp3:okhttp:${BuildVersions.okhttpVersion}")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:${BuildVersions.okhttpVersion}")
    implementation("org.jsoup:jsoup:1.13.1")

    // Logging
    implementation("com.jakewharton.timber:timber:${BuildVersions.timberVersion}")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:${BuildVersions.nav_version}")
    implementation("androidx.navigation:navigation-ui-ktx:${BuildVersions.nav_version}")

    // Export
    implementation("com.opencsv:opencsv:5.4") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    /// ODF
    implementation("org.odftoolkit:odfdom-java:0.8.7")
    implementation("com.fasterxml.woodstox:woodstox-core:6.2.6")

    // Dependency Injection
    implementation("io.insert-koin:koin-android:3.1.2")
}
