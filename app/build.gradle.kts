plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// Auto-increment versionCode from git commit count
val gitVersionCode: Int by lazy {
    try {
        val process = Runtime.getRuntime().exec("git rev-list --count HEAD")
        process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
    } catch (e: Exception) {
        1
    }
}

val gitVersionName: String by lazy {
    try {
        val count = Runtime.getRuntime().exec("git rev-list --count HEAD")
            .inputStream.bufferedReader().readText().trim()
        val sha = Runtime.getRuntime().exec("git rev-parse --short HEAD")
            .inputStream.bufferedReader().readText().trim()
        "0.1.0-build$count ($sha)"
    } catch (e: Exception) {
        "0.1.0"
    }
}

android {
    namespace = "com.underscore.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.underscore.app"
        minSdk = 26
        targetSdk = 34
        versionCode = gitVersionCode
        versionName = gitVersionName
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = "underscore"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Spotify App Remote SDK — download the AAR from:
    // https://github.com/spotify/android-sdk/releases
    // Place spotify-app-remote-release-0.8.0.aar in app/libs/
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp for API calls (Gemini, Spotify Web API, Weather)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Room for local DB (narrative-tagged song cache)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // Chrome Custom Tabs (for Spotify auth browser flow)
    implementation("androidx.browser:browser:1.7.0")
}
