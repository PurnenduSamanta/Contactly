// :core:common — Cross-cutting Pure Kotlin module
// Contains: Enums, pure utilities, interfaces, StatusEventBus
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Coroutines (for StatusEventBus's SharedFlow)
    implementation(libs.kotlinx.coroutines.core)
}
