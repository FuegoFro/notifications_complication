plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.fuegofro.notifications_complication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fuegofro.notifications_complication"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(project(mapOf("path" to ":common")))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.watchface.complications.data)
    implementation(libs.androidx.watchface.complications.data.source)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.wearable)

    // implementation("androidx.percentlayout:percentlayout:1.0.0")
    // implementation("androidx.legacy:legacy-support-v4:1.0.0")
    // implementation("androidx.recyclerview:recyclerview:1.3.0")
}
