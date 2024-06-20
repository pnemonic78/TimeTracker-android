plugins {
    aliasId(libs.plugins.androidApplication)
    aliasId(libs.plugins.hilt)
    aliasId(libs.plugins.kapt)
    aliasId(libs.plugins.kotlinAndroid)
    aliasId(libs.plugins.kotlinParcelize)
    aliasId(libs.plugins.google.services)
    aliasId(libs.plugins.crashlytics)
    alias(libs.plugins.compose.compiler)
}

val versionMajor = project.properties["APP_VERSION_MAJOR"].toString().toInt()
val versionMinor = project.properties["APP_VERSION_MINOR"].toString().toInt()

android {
    namespace = "com.tikalk.worktracker"
    compileSdk = Android.Version.compileSdk

    defaultConfig {
        applicationId = "com.tikalk.worktracker"
        minSdk = Android.Version.minSdk
        targetSdk = Android.Version.targetSdk
        versionCode = versionMajor * 1000 + versionMinor
        versionName = "${versionMajor}." + versionMinor.toString().padStart(2, '0')

        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "API_URL", "\"https://time.infra.tikalk.dev/\"")
        buildConfigField("Boolean", "LOCATION", "false")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = project.properties["STORE_PASSWORD_RELEASE"].toString()
            keyAlias = "release"
            keyPassword = project.properties["KEY_PASSWORD_RELEASE"].toString()
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // disabled until fix proguard issues: minifyEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
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

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.kotlin_module"

            pickFirsts += "META-INF/plexus/*"
            pickFirsts += "META-INF/sisu/*"
            pickFirsts += "plugin.properties"
        }
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.model)

    // Jetpack
    implementation(libs.jetpack.appcompat)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.jetpack)

    // Database
    implementation(libs.db.room.kotlin)
    kapt(libs.db.room.compiler)

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

    // Export
    implementation(libs.doc.csv) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(libs.kotlin.html)
    implementation(libs.doc.odf) {
        exclude(group = "io.github.git-commit-id", module = "git-commit-id-maven-plugin")
    }
    implementation(libs.doc.odfXML)
    implementation(libs.doc.woodstox)

    // Dependency Injection
    implementation(libs.di.hilt)
    kapt(libs.di.hilt.compiler)

    // Testing
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}
