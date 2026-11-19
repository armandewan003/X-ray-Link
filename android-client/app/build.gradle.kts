plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.xraylink.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xraylink.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        val customApiUrl = (project.findProperty("XRAY_API_BASE_URL") as? String)
            ?: System.getenv("XRAY_API_BASE_URL")
        val customWsUrl = (project.findProperty("XRAY_WS_BASE_URL") as? String)
            ?: System.getenv("XRAY_WS_BASE_URL")

        debug {
            val debugApi = customApiUrl?.takeIf { it.isNotBlank() } ?: "http://10.0.2.2:3000/api/v1/"
            val debugWs = customWsUrl?.takeIf { it.isNotBlank() } ?: "ws://10.0.2.2:3000/ws"
            buildConfigField("String", "API_BASE_URL", "\"$debugApi\"")
            buildConfigField("String", "WS_BASE_URL", "\"$debugWs\"")
        }
        release {
            val releaseApi = customApiUrl?.takeIf { it.isNotBlank() } ?: "https://api.xraylink.example.com/api/v1/"
            val releaseWs = customWsUrl?.takeIf { it.isNotBlank() } ?: "wss://api.xraylink.example.com/ws"
            buildConfigField("String", "API_BASE_URL", "\"$releaseApi\"")
            buildConfigField("String", "WS_BASE_URL", "\"$releaseWs\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!System.getenv("KEYSTORE_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Networking & Security
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WireGuard Official Android Tunnel Library
    implementation("com.wireguard.android:tunnel:1.0.20230706")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.55")
    kapt("com.google.dagger:hilt-compiler:2.55")
}
