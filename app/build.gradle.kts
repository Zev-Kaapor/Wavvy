// Build configuration for the app module

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "com.wavvy.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wavvy.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
        val rawGoogleClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        val googleClientId = rawGoogleClientId.replace("\"", "")

        debug {
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // AndroidX & Compose Platform
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    // UI Components & Image Loading
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.reorderable.list)

    // Dependency Injection (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Storage, Network & Identity
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)
    implementation(libs.credentials.core)
    implementation(libs.credentials.play)
    implementation(libs.google.identity)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.json)
    implementation(libs.gson)

    // Media Streaming & Player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    // NewPipe Extractor — YouTube signature deobfuscation fallback
    implementation(libs.newpipe.extractor)

    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.jsoup)
}
