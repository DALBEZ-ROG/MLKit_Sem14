plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.uteq.software.mlkit_sem14"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.uteq.software.mlkit_sem14"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Face features
    implementation("com.google.mlkit:face-detection:16.1.7")
    // Text features
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:18.0.2")
    // Barcode & QR features
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // CameraX (escaneo en vivo)
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
}