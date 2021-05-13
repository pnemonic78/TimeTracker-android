plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

val versionMajor = (project.properties["APP_VERSION_MAJOR"] as String).toInt()
val versionMinor = (project.properties["APP_VERSION_MINOR"] as String).toInt()

android {
    compileSdkVersion(BuildVersions.compileSdkVersion)

    defaultConfig {
        applicationId("com.tikalk.worktracker")
        minSdkVersion(BuildVersions.minSdkVersion)
        targetSdkVersion(BuildVersions.targetSdkVersion)
        versionCode = versionMajor * 1000 + versionMinor
        versionName = "${versionMajor}." + versionMinor.toString().padStart(2, '0')
        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")

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
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/*.kotlin_module")
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${BuildVersions.kotlin_version}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:${BuildVersions.kotlin_version}")

    // Google Support
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.4.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha03")

    // Testing
    androidTestImplementation("androidx.test:core:1.3.0")
    androidTestImplementation("androidx.test:runner:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    testImplementation("junit:junit:${BuildVersions.junitVersion}")

    // Database
    implementation("androidx.room:room-common:${BuildVersions.roomVersion}")
    implementation("androidx.room:room-runtime:${BuildVersions.roomVersion}")
    implementation("androidx.room:room-rxjava2:${BuildVersions.roomVersion}")
    kapt("androidx.room:room-compiler:${BuildVersions.roomVersion}")

    // Rx
    implementation("io.reactivex.rxjava2:rxandroid:${BuildVersions.rxAndroidVersion}")
    implementation("io.reactivex.rxjava2:rxkotlin:${BuildVersions.rxKotlinVersion}")

    // Web
    implementation("com.squareup.retrofit2:retrofit:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.retrofit2:adapter-rxjava2:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.retrofit2:converter-gson:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.retrofit2:converter-scalars:${BuildVersions.retrofit2Version}")
    implementation("com.squareup.okhttp3:logging-interceptor:${BuildVersions.okhttpVersion}")
    implementation("com.squareup.okhttp3:okhttp:${BuildVersions.okhttpVersion}")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:${BuildVersions.okhttpVersion}")
    implementation("com.google.code.gson:gson:2.8.6")
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
    implementation("io.insert-koin:koin-android:3.0.1")
}
