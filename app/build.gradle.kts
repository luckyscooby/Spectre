plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hygnus.spectre"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hygnus.spectre"
        minSdk = 28
        targetSdk = 34
        versionCode = 43
        versionName = "1.8.1.241129"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionNameSuffix = "_Dev-Graphite"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.datastore.core.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}