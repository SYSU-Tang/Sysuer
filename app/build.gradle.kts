plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.sysu.edu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sysu.edu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1927
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
//            firebaseCrashlytics {
//                mappingFileUploadEnabled = false
//                nativeSymbolUploadEnabled = false
//            }
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        compose = true
        aidl = true
    }
    sourceSets {
        getByName("main") {
            java {
                srcDirs(
                    "src\\main\\java"
                )
            }
        }
    }
}

dependencies {

    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.fastjson2)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation(libs.activity)
    implementation(libs.annotation)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.preference)
    implementation(libs.work.runtime)
    implementation(libs.material.preference) {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.dev.material) {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.api)
    implementation(libs.provider)
    //api(libs.wechat.sdk.android)
}