// :platform:geofence — Data Layer (Android)
// Contains: ContactlyGeofenceManager, GeofenceBroadcastReceiver
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.purnendu.contactly.geofence"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":platform:notification"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Google Play Services - Location (Geofencing)
    implementation(libs.play.services.location)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}
