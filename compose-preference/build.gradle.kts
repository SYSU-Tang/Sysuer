plugins {
	alias(libs.plugins.android.library)
}

android {
	namespace = "com.miyuyan.preference"
	compileSdk {
		version = release(37)
	}

	defaultConfig {
		minSdk = 16
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_25
		targetCompatibility = JavaVersion.VERSION_25
	}
	buildToolsVersion = "37.0.0"

}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.appcompat)
	implementation(libs.google.material)
	testImplementation(libs.junit)
	androidTestImplementation(libs.espresso.core)
	androidTestImplementation(libs.ext.junit)
}