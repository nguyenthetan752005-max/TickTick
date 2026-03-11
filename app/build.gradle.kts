plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hcmute.edu.vn.nguyenthetan"
    compileSdk = 36 // Đã nâng cấp để hỗ trợ các thư viện mới nhất

    defaultConfig {
        applicationId = "hcmute.edu.vn.nguyenthetan"
        minSdk = 24
        targetSdk = 36 // Đồng bộ với compileSdk
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
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
}