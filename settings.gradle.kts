pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Contactly"

// :app — Presentation Layer (Application wiring)
include(":app")

// :core — Foundation modules
include(":core:common")          // ⚪ Cross-cutting — Pure Kotlin (Enums, utils, interfaces)
include(":core:domain")          // 🟡 Domain Layer — Pure Kotlin (Models, Use Cases, Repository interfaces)
include(":core:data")            // 🟧 Data Layer — Room, DataStore, Repository implementations
include(":core:network")         // 🟧 Data Layer — Ktor client, API calls
include(":core:ui")              // 🟦 Presentation Layer — Theme, shared composables

// :feature — Feature modules
include(":feature:home")         // 🟦 Presentation Layer — Home screen, HomeViewModel
include(":feature:settings")     // 🟦 Presentation Layer — Settings screen, SettingsViewModel

// :platform — Platform integration modules
include(":platform:alarm")       // 🟧 Data Layer — AlarmManager, receivers
include(":platform:geofence")    // 🟧 Data Layer — Geofencing, receiver
include(":platform:notification")// 🟧 Data Layer — Notification channels & helpers