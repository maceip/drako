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
    implementation("androidx.compose.ui:ui-util:$composeAlpha")
    implementation("androidx.compose.foundation:foundation:$composeAlpha")
    implementation("androidx.compose.animation:animation:$composeAlpha")
    implementation("androidx.compose.animation:animation-graphics:$composeAlpha")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeAlpha")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:$composeAlpha")

    // 2. Material 3
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")
    implementation("androidx.compose.material:material-icons-core:1.3.0")

    // 3. Activity & Lifecycle (The "Bridges")
    implementation("androidx.activity:activity-compose:1.13.0-alpha01")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.savedstate:savedstate:1.3.0-alpha05")

    // 4. High-end Graphics
    implementation("androidx.graphics:graphics-core:1.0.4")

    // 5. Coroutines for ResourceMonitor
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // 6. Core KTX
    implementation("androidx.core:core-ktx:1.15.0")

    // 7. Haze for backdrop blur effects
    implementation("dev.chrisbanes.haze:haze:1.7.1")
    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")

    // 8. Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}