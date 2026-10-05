plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.yolokitmobile"
    ndkVersion = "28.2.13676358"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.yolokitmobile"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes += listOf("**/libQnnDsp*.so", "**/libQnnGpu.so", "**/libQnnHtpPrepare.so")
        }
    }
    androidResources {
        noCompress += listOf("param", "bin")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("io.github.gd182:yolokit")
    testImplementation(libs.junit)
}
