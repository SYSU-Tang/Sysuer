plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "com.miyuyan.preference"
	compileSdk {
		version = release(37)
	}

	defaultConfig {
		minSdk = 23
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_25
		targetCompatibility = JavaVersion.VERSION_25
	}
	buildToolsVersion = "37.0.0"
	buildFeatures {
		compose = true
	}

}

dependencies {
	implementation(platform(libs.compose.bom))
	implementation(libs.activity.compose)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.runtime)
	implementation(libs.androidx.ui)
	implementation(libs.appcompat)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.navigation3.runtime)
//	implementation(libs.lifecycle.viewmodel.ktx)
	implementation(libs.ui.graphics)
	implementation(libs.androidx.preference.ktx)
	implementation(libs.ui.tooling.preview)
	testImplementation(libs.junit)
	implementation(libs.material.icons.extended)
	androidTestImplementation(libs.espresso.core)
	androidTestImplementation(libs.ext.junit)
	androidTestImplementation(libs.ui.test.junit4)
	debugImplementation(libs.ui.test.manifest)
	debugImplementation(libs.ui.tooling)
}