plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
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
        versionCode = 1930
        versionName = "1.0.8(beta3)"
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
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        compose = true
        aidl = true
    }
    sourceSets {
        getByName("main") {
            java {
                val directories: MutableSet<String> = mutableSetOf(
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
    implementation(libs.material.preference)
    {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.dev.material)
    {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.core)
    implementation(libs.ext.tables)
    implementation(libs.ext.strikethrough)
    implementation(libs.google.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.api)
    implementation(libs.provider)
    implementation(libs.html)
//    implementation("dev.rikka.rikkax.appcompat:appcompat:1.6.1")
    /*configurations.all {
        exclude("androidx.appcompat", "appcompat")
    }*/
//    implementation(libs.androidx.preference.material3)
//    implementation("com.github.knightwood:material3-preference:1.4")
    //api(libs.wechat.sdk.android)
}