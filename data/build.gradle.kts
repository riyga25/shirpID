plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "by.riyga.shirpid.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlin {
        jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    }
}

dependencies {
    implementation(libs.insert.koin.koinCore)
    implementation(libs.insert.koin.koinAndroid)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.squareup.okhttp3)
    implementation(libs.jakewharton.retrofit.retrofit2KotlinxSerializationConverter)
    implementation(libs.squareup.okhttp3.loggingInterceptor)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // android location
    implementation(libs.google.android.gms.playServicesLocation)
    implementation(libs.kotlinx.coroutines.playServices)
}