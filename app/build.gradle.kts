plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.riyga.identifier"

    defaultConfig {
        applicationId = "by.riyga.birdnet"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    aaptOptions {
        noCompress += "tflite"
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.tensorflow.lite)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.squareup.okhttp3)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.toolingPreview)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.eygraber.uri.kmp)

    implementation(libs.google.android.gms.playServicesLocation)
    implementation(libs.kotlinx.coroutines.playServices)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.insert.koin.koinCore)
    implementation(libs.insert.koin.koinAndroid)
    implementation(libs.insert.koin.koinCompose)
    implementation(libs.insert.koin.koinComposeViewmodel)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jakewharton.retrofit.retrofit2KotlinxSerializationConverter)
    implementation(libs.squareup.okhttp3.loggingInterceptor)

    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlyticsNdk)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}