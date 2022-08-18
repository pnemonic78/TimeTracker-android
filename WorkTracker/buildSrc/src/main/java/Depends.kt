object Android {
    object Version {
        const val compileSdk = 32
        const val minSdk = 23
        const val targetSdk = 32

        const val compose = "1.2.1"
        const val hilt = "2.42"
        const val koin = "3.1.5"
        const val navigation = "2.5.0"
        const val okhttp = "4.9.3"
        const val preference = "1.2.0"
        const val retrofit = "2.9.0"
        const val room = "2.4.2"
        const val rxAndroid = "3.0.0"
        const val rxKotlin = "3.0.1"
        const val security = "1.1.0-alpha03"
        const val test = "1.4.0"
    }

    object Database {
        const val roomCompiler = "androidx.room:room-compiler:${Version.room}"
        const val roomKtx = "androidx.room:room-ktx:${Version.room}"
        const val roomRx = "androidx.room:room-rxjava3:${Version.room}"
    }

    object Image {
        const val coil = "io.coil-kt:coil-compose:1.3.2"
        const val ratingbar = "io.github.a914-gowtham:compose-ratingbar:1.2.2"
    }

    // Dependency Injection
    object Inject {
        const val hilt = "com.google.dagger:hilt-android:${Version.hilt}"
        const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Version.hilt}"
        const val koin = "io.insert-koin:koin-android:${Version.koin}"
    }

    object Jetpack {
        const val appcompat = "androidx.appcompat:appcompat:1.5.0"
        const val material = "androidx.compose.material:material:${Version.compose}"
        const val livedata = "androidx.compose.runtime:runtime-livedata:${Version.compose}"
        const val composeTooling = "androidx.compose.ui:ui-tooling:${Version.compose}"
        const val compose = "androidx.compose.ui:ui:${Version.compose}"
        const val constraint_layout = "androidx.constraintlayout:constraintlayout:2.1.4"
        const val core = "androidx.core:core-ktx:1.7.0"
        const val navigationCompose = "androidx.navigation:navigation-compose:${Version.navigation}"
        const val navigationFragment = "androidx.navigation:navigation-fragment-ktx:${Version.navigation}"
        const val navigationUI = "androidx.navigation:navigation-ui-ktx:${Version.navigation}"
        const val preference = "androidx.preference:preference-ktx:${Version.preference}"
        const val security = "androidx.security:security-crypto-ktx:${Version.security}"
    }

    object JSON {
        const val kotlin = Kotlin.JSON.json
        const val retrofit = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0"
    }

    object Logging {
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

    object Reactive {
        const val retrofit = "com.squareup.retrofit2:adapter-rxjava3:${Version.retrofit}"
        const val rxandroid="io.reactivex.rxjava3:rxandroid:${Version.rxAndroid}"
        const val rxkotlin="io.reactivex.rxjava3:rxkotlin:${Version.rxKotlin}"
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
        const val jsoup = "1.15.2"
        const val odf = "0.10.0"
        const val opencsv = "5.6"
        const val woodstox = "6.3.1"
    }

    object Document {
        const val opencsv = "com.opencsv:opencsv:${Version.opencsv}"
        const val odf = "org.odftoolkit:odfdom-java:${Version.odf}"
        const val odfXML = "xml-apis:xml-apis:1.4.01"
        const val woodstox = "com.fasterxml.woodstox:woodstox-core:${Version.woodstox}"
    }

    object Network {
        const val jsoup = "org.jsoup:jsoup:${Version.jsoup}"
    }
}

object Kotlin {
    object Version {
        const val html = "0.8.0"
        const val kotlin = "1.6.21"
        const val serialization = "1.3.33"
    }

    object Document {
        const val html = "org.jetbrains.kotlinx:kotlinx-html-jvm:${Version.html}"
    }

    object JSON {
        const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.serialization}"
    }
}