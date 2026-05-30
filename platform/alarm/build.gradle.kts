// :platform:alarm — Data Layer (Android)
// Contains: ContactlyAlarmManager, AliasAlarmReceiver, ReActivationReceiver
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.purnendu.contactly.alarm"
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
    implementation(project(":platform:notification"))

    // Gson (for alarm metadata JSON)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Koin (for KoinComponent in receivers)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
