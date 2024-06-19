plugins {
    aliasId(libs.plugins.androidApplication)
    aliasId(libs.plugins.hilt)
    aliasId(libs.plugins.kotlinAndroid)
    aliasId(libs.plugins.kapt)
    aliasId(libs.plugins.kotlinParcelize)
    aliasId(libs.plugins.google.services)
    aliasId(libs.plugins.crashlytics)
}

val versionMajor = (project.properties["APP_VERSION_MAJOR"] as String).toInt()
val versionMinor = (project.properties["APP_VERSION_MINOR"] as String).toInt()

android {
    compileSdk = Android.Version.compileSdk
    namespace = "com.tikalk.worktracker"

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
            storePassword = project.properties["STORE_PASSWORD_RELEASE"] as String
            keyAlias = "release"
            keyPassword = project.properties["KEY_PASSWORD_RELEASE"] as String
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

    composeOptions {
        kotlinCompilerExtensionVersion = Android.Version.composeCompiler
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
    implementation(project(":core"))
    implementation(project(":model"))

    // Jetpack
    implementation(libs.jetpack.appcompat)
    implementation(libs.compose.activity)
    implementation(Android.Jetpack.composeConstraintLayout)
    implementation(Android.Jetpack.composeIcons)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.tooling)
    implementation(libs.compose.preview)
    implementation(Android.Jetpack.constraintLayout)
    implementation(libs.jetpack.core)
    implementation(Android.Jetpack.material3)
    implementation(Android.Jetpack.preference)

    // Database
    implementation(libs.db.room.kotlin)
    kapt(libs.db.room.compiler)

    // Rx
    implementation(Kotlin.Reactive.coroutinesAndroid)

    // Web
    implementation(libs.net.okhttp)
    implementation(libs.net.okhttp.logging)
    implementation(Android.Network.okhttp_url)
    implementation(libs.net.retrofit)
    implementation(Android.Network.retrofit_scalars)
    implementation(Java.Network.jsoup)

    // Logging
    implementation(libs.logging.timber)
    implementation(Android.Logging.crashlytics)

    // Navigation
    implementation(Android.Jetpack.navigationCompose)
    implementation(libs.jetpack.navigationFragment)
    implementation(Android.Jetpack.navigationUI)

    // Export
    implementation(Java.Document.opencsv) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(Kotlin.Document.html)
    implementation(Java.Document.odfJava) {
        exclude(group = "io.github.git-commit-id", module = "git-commit-id-maven-plugin")
    }
    implementation(Java.Document.odfXML)
    implementation(Java.Document.woodstox)

    // Dependency Injection
    implementation(libs.di.hilt)
    kapt(libs.di.hilt.compiler)

    // Testing
    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
}
