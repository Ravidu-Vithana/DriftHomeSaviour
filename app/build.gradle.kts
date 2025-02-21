plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ryvk.drifthomesaviour"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ryvk.drifthomesaviour"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // Add the dependency for the Firebase Authentication library
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Add the dependency for the Google Maps API
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Add the dependency for the Google Maps Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Add the dependency for the Firebase Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Add the dependency for the Firebase Storage
    implementation("com.google.firebase:firebase-storage")

    // Add OKHttp client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //add GSON
    implementation ("com.google.code.gson:gson:2.11.0")

    //add glide for GIF manipulation
    implementation ("com.github.bumptech.glide:glide:4.15.1")
}