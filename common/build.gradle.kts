plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.fuegofro.notifications_complication.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        vectorDrawables { useSupportLibrary = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.0")
}
