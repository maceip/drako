plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) // This now carries the weight for the AI UI
}

android {
    namespace = "ai.drako"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.drako"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ... [buildTypes, compileOptions as before] ...

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.geometry)
    // 1. Core Compose (Must be perfectly synced)
    val composeAlpha = "1.11.0-alpha03"
    implementation("androidx.compose.ui:ui:$composeAlpha")
    implementation("androidx.compose.foundation:foundation:$composeAlpha")
    implementation("androidx.compose.animation:animation:$composeAlpha")
    implementation("androidx.compose.animation:animation-graphics:$composeAlpha")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeAlpha")
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")
    implementation("androidx.compose.material:material-icons-core:1.3.0")

    // 2. Material 3 (Must match the alpha-12 spec for shared element fixes)
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")

    // 3. Activity & Lifecycle (The "Bridges")
    // Note: Activity 1.10.0-alpha03 is required for Compose 1.11.0 node compatibility
    implementation("androidx.activity:activity-compose:1.13.0-alpha01")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // 4. High-end Graphics
    implementation("androidx.graphics:graphics-core:1.0.4")
}