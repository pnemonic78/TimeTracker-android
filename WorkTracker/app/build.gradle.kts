plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val versionMajor = project.properties["APP_VERSION_MAJOR"].toString().toInt()
val versionMinor = project.properties["APP_VERSION_MINOR"].toString().toInt()

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

    // Database
    implementation(Android.Database.roomKotlin)
    kapt(Android.Database.roomCompiler)

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
    implementation(Android.Inject.hilt)
    kapt(Android.Inject.hiltCompiler)

    // Testing
    testImplementation(Android.Test.junit)
    androidTestImplementation(Android.Test.junit_ext)
    androidTestImplementation(Android.Test.espresso_core)

    // Miscellaneous
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
}
