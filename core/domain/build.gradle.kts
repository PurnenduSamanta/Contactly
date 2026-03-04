// :core:domain — Domain Layer (Pure Kotlin)
// Contains: Models, Repository Interfaces, Use Cases
// ❌ NO android.* imports allowed here
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Domain depends on common (for enums like ActivationMode, ViewMode, etc.)
    implementation(project(":core:common"))

    // Coroutines (for Flow in repository interfaces)
    implementation(libs.kotlinx.coroutines.core)
}
