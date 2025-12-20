plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "by.riyga.shirpid"

    defaultConfig {
        applicationId = "by.riyga.shirpid"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.0.0-alpha03"
        resourceConfigurations += setOf("ru", "en")

        ndk {
            abiFilters.addAll(arrayListOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    signingConfigs {
        create("signingConfigRelease") {
            storeFile = file("signing/key.jks")
            storePassword = project.properties["RELEASE_STORE_PASSWORD"].toString()
            keyAlias = project.properties["RELEASE_KEY_ALIAS"].toString()
            keyPassword = project.properties["RELEASE_KEY_PASSWORD"].toString()
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("signingConfigRelease")
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
    implementation(project(":presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.splashScreen)

    implementation(libs.androidx.activity.compose)

    implementation(libs.insert.koin.koinCore)
    implementation(libs.insert.koin.koinAndroid)

    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.crashlyticsNdk)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.perf)
}