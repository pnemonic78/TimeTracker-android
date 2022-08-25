plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val versionMajor = (project.properties["APP_VERSION_MAJOR"] as String).toInt()
val versionMinor = (project.properties["APP_VERSION_MINOR"] as String).toInt()

android {
    compileSdk = Android.Version.compileSdk

    defaultConfig {
        applicationId = "com.tikalk.worktracker"
        minSdk = Android.Version.minSdk
        targetSdk = Android.Version.targetSdk
        versionCode = versionMajor * 1000 + versionMinor
        versionName = "${versionMajor}." + versionMinor.toString().padStart(2, '0')

        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "API_URL", "\"https://time.infra.tikalk.dev/\"")
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

            pickFirsts += "META-INF/plexus/*"
            pickFirsts += "META-INF/sisu/*"
            pickFirsts += "plugin.properties"
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Jetpack
    implementation(Android.Jetpack.appcompat)
    implementation(Android.Jetpack.constraint_layout)
    implementation(Android.Jetpack.preference)
    implementation(Android.Jetpack.security)

    // Database
    implementation(Android.Database.roomRx)
    kapt(Android.Database.roomCompiler)

    // Rx
    implementation(Android.Reactive.retrofit)
    implementation(Android.Reactive.rxandroid)
    implementation(Android.Reactive.rxkotlin)

    // Web
    implementation(Android.Network.logging)
    implementation(Android.Network.okhttp)
    implementation(Android.Network.okhttp_url)
    implementation(Android.Network.retrofit)
    implementation(Android.Network.retrofit_scalars)
    implementation(Java.Network.jsoup)

    // Logging
    implementation(Android.Logging.timber)
    implementation("com.google.firebase:firebase-crashlytics:18.2.12")

    // Navigation
    implementation(Android.Navigation.navigation_fragment)
    implementation(Android.Navigation.navigation_ui)

    // Export
    implementation(Java.Document.opencsv) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(Kotlin.Document.html)
    implementation(Java.Document.odf) {
        exclude(group = "io.github.git-commit-id", module = "git-commit-id-maven-plugin")
    }
    implementation(Java.Document.odfXML)
    implementation(Java.Document.woodstox)

    // Dependency Injection
    implementation(Android.Inject.koin)

    // Testing
    testImplementation(Android.Test.junit)
    androidTestImplementation(Android.Test.junit_ext)
    androidTestImplementation(Android.Test.espresso_core)

    // Miscellaneous
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
}
