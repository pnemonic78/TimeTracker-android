import org.gradle.api.JavaVersion

object Android {
    object Version {
        const val compileSdk = 34
        const val minSdk = 23
        const val targetSdk = 33

        const val compose = "1.3.3"
        const val composeCompiler = "1.5.3"
        const val composeMaterial = "1.3.1"
        const val hilt = "2.48"
        const val navigation = "2.5.0"
        const val okhttp = "4.11.0"
        const val preference = "1.2.0"
        const val retrofit = "2.9.0"
        const val room = "2.6.0-beta01"
        const val security = "1.1.0-alpha03"
        const val test = "1.4.0"
    }

    object Database {
        const val roomCompiler = "androidx.room:room-compiler:${Version.room}"
        const val roomKotlin = "androidx.room:room-ktx:${Version.room}"
    }

    object Image {
        const val coil = "io.coil-kt:coil-compose:2.4.0"
        const val ratingbar = "io.github.a914-gowtham:compose-ratingbar:1.2.2"
    }

    // Dependency Injection
    object Inject {
        const val hilt = "com.google.dagger:hilt-android:${Version.hilt}"
        const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Version.hilt}"
    }

    object Jetpack {
        const val appcompat = "androidx.appcompat:appcompat:1.7.0-alpha02"
        const val livedata = "androidx.compose.runtime:runtime-livedata:${Version.compose}"
        const val composeActivity = "androidx.activity:activity-compose:1.6.1"
        const val composeCompiler = "androidx.compose.compiler:compiler:${Version.composeCompiler}"
        const val composeIcons = "androidx.compose.material:material-icons-extended:${Version.composeMaterial}"
        const val composeMaterial = "androidx.compose.material:material:${Version.composeMaterial}"
        const val composeRuntime = "androidx.compose.runtime:runtime-livedata:${Version.compose}"
        const val composeUi = "androidx.compose.ui:ui:${Version.compose}"
        const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Version.compose}"
        const val composeUiToolingPreview = "androidx.compose.ui:ui-tooling-preview:${Version.compose}"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.1.4"
        const val composeConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:1.0.1"
        const val core = "androidx.core:core-ktx:1.12.0"
        const val navigationCompose = "androidx.navigation:navigation-compose:${Version.navigation}"
        const val navigationFragment = "androidx.navigation:navigation-fragment-ktx:${Version.navigation}"
        const val navigationUI = "androidx.navigation:navigation-ui-ktx:${Version.navigation}"
        const val preference = "androidx.preference:preference-ktx:${Version.preference}"
        const val security = "androidx.security:security-crypto-ktx:${Version.security}"
    }

    object JSON {
        const val kotlin = Kotlin.JSON.json
        const val retrofit = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0"
    }

    object Logging {
        const val crashlytics = "com.google.firebase:firebase-crashlytics:18.3.7"
        const val timber = "com.jakewharton.timber:timber:5.0.1"
    }

    object Navigation {
        const val navigation_fragment = "androidx.navigation:navigation-fragment-ktx:${Version.navigation}"
        const val navigation_ui = "androidx.navigation:navigation-ui-ktx:${Version.navigation}"
    }

    object Network {
        const val logging = "com.squareup.okhttp3:logging-interceptor:${Version.okhttp}"
        const val okhttp = "com.squareup.okhttp3:okhttp:${Version.okhttp}"
        const val okhttp_url = "com.squareup.okhttp3:okhttp-urlconnection:${Version.okhttp}"
        const val retrofit = "com.squareup.retrofit2:retrofit:${Version.retrofit}"
        const val retrofit_scalars = "com.squareup.retrofit2:converter-scalars:${Version.retrofit}"
    }

    object Test {
        const val junit = "junit:junit:4.13.2"
        const val junit_ext = "androidx.test.ext:junit:1.1.3"
        const val espresso_core = "androidx.test.espresso:espresso-core:3.4.0"
        const val runner = "androidx.test:runner:${Version.test}"
        const val rules = "androidx.test:rules:${Version.test}"
    }
}

object Java {
    object Version {
        val jvm = JavaVersion.VERSION_17

        const val jsoup = "1.16.1"
        // odf "0.10.0" and "0.11.0" cause "java.nio.charset.IllegalCharsetNameException: java.nio.charset.CharsetICU[UTF-8]"
        const val odf = "0.8.7"
        const val opencsv = "5.8"
        const val woodstox = "6.5.1"
    }

    object Document {
        const val opencsv = "com.opencsv:opencsv:${Version.opencsv}"
        const val odfJava = "org.odftoolkit:odfdom-java:${Version.odf}"
        const val odfXML = "xml-apis:xml-apis:1.4.01"
        const val woodstox = "com.fasterxml.woodstox:woodstox-core:${Version.woodstox}"
    }

    object Network {
        const val jsoup = "org.jsoup:jsoup:${Version.jsoup}"
    }
}

object Kotlin {
    object Version {
        const val coroutines = "1.7.3"
        const val html = "0.9.1"
        const val kotlin = "1.9.10"
        const val serialization = "1.6.0"
    }

    object Document {
        const val html = "org.jetbrains.kotlinx:kotlinx-html-jvm:${Version.html}"
    }

    object JSON {
        const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.serialization}"
    }

    object Reactive {
        const val coroutinesAndroid =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"
    }
}