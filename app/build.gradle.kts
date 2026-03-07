plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hcmute.edu.vn.nguyenthetan"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "hcmute.edu.vn.nguyenthetan"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.androidx.room.runtime)
    implementation("androidx.core:core-splashscreen:1.0.1")
    annotationProcessor(libs.androidx.room.compiler)
}