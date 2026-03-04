// :platform:notification — Data Layer (Android)
// Contains: NotificationHelper (channels & sending)
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.purnendu.contactly.platform.notification"
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

    // AndroidX Core (for NotificationCompat)
    implementation(libs.androidx.core.ktx)
}
