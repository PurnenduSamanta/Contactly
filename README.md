<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="120" alt="Contactly Logo"/>
</p>

<h1 align="center">Contactly</h1>

<p align="center">
  <b>Schedule temporary nicknames & photos for your contacts — automatically.</b>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.purnendu.contactly">
    <img src="https://img.shields.io/badge/Google%20Play-Download-green?style=for-the-badge&logo=google-play" alt="Get it on Google Play"/>
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue?style=flat-square" alt="Min SDK 24"/>
  <img src="https://img.shields.io/badge/Target%20SDK-36-blue?style=flat-square" alt="Target SDK 36"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square&logo=jetpack-compose&logoColor=white" alt="Jetpack Compose"/>
</p>

---

## 🎭 What is Contactly?

Ever wanted to temporarily change a contact's name or photo on your phone — and have it automatically switch back? **Contactly** lets you do exactly that.

Set a **schedule** with a temporary name (and optional photo), pick the days and times, and Contactly handles the rest. When the scheduled time arrives, your contact's name and photo are swapped. When the time ends, everything is restored to normal — like nothing ever happened.

**Use cases:**
- 🎉 Prank your friends by changing their caller ID name during specific hours
- 🏢 Show a work-friendly display name during office hours
- 🎮 Set fun gaming aliases for your contacts during game nights
- 🎁 Surprise someone with a special nickname on their birthday

---

## ✨ Features

### Core
- **Scheduled Name Swapping** — Set a temporary name for any contact with automatic apply & revert
- **Photo Swapping** — Optionally change the contact's profile photo alongside the name
- **One-Time Schedules** — Run once and auto-clean up after reverting
- **Repeating Schedules** — Repeat weekly on selected days (Mon–Sun)
- **Instant Restore on Delete** — If you delete a schedule, the contact is immediately restored to its original state
- **Active State Indicator** — See at a glance which schedules are currently active (temporary name applied)

### UI & Experience
- **List & Grid Views** — Toggle between list and grid layouts for your schedules
- **Sliding Image Carousel** — Preview both original and temporary contact photos
- **Day Picker Chips** — Visual day-of-week selection with intuitive chips
- **Expressive Animations** — Smooth press animations and micro-interactions throughout the app
- **Custom Bottom Navigation** — Modern FAB-centered navigation bar with semi-circle cutout

### Settings & Security
- **Theme Support** — Light, Dark, and System-default themes
- **Biometric App Lock** — Secure the app with fingerprint or face authentication
- **Schedule Notifications** — Get notified when a name swap happens (toggle on/off)
- **In-App Updates** — Seamless updates via Google Play's in-app update API
- **Feedback & Privacy Policy** — Built-in WebView screens for user support

### Reliability
- **Boot-Safe Alarms** — All schedules are automatically re-registered after device reboot
- **Exact Alarms** — Uses `setExactAndAllowWhileIdle` for precise timing
- **Alarm Event Bus** — Real-time UI refresh when alarms fire (no polling)

---

## 🏗️ Architecture

Contactly follows a clean, layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────┐
│                   UI Layer                  │
│  Composables  ←──  ViewModels (StateFlow)   │
├─────────────────────────────────────────────┤
│               Domain / Logic                │
│   ContactlyAlarmManager · ImageStorage      │
│   AlarmEventBus · BiometricHelper           │
├─────────────────────────────────────────────┤
│                Data Layer                   │
│  ContactsRepository · SchedulesRepository   │
│  Room Database · DataStore Preferences      │
├─────────────────────────────────────────────┤
│                 Platform                    │
│  AliasAlarmReceiver · RescheduleReceiver    │
│  ContentResolver · AlarmManager             │
└─────────────────────────────────────────────┘
```

### Key Design Decisions

| Aspect | Approach |
|---|---|
| **DI** | Koin — lightweight, Kotlin-first |
| **UI** | 100% Jetpack Compose with Material 3 |
| **State** | `StateFlow` + `collectAsStateWithLifecycle` |
| **DB** | Room (schedules + alarm metadata) |
| **Preferences** | DataStore (theme, view mode, biometric, notifications) |
| **Alarms** | `AlarmManager` exact alarms with `BroadcastReceiver` |
| **Contacts** | Android `ContactsContract` via `ContentResolver` |
| **Testability** | All ViewModels depend on interfaces, not Android classes |

---

## 🛠️ Tech Stack

| Category | Libraries |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation (type-safe with Kotlin Serialization) |
| Dependency Injection | Koin |
| Local Database | Room |
| Preferences | DataStore |
| Image Loading | Coil |
| Permissions | Accompanist Permissions |
| Serialization | Gson (alarm metadata) · Kotlinx Serialization (navigation) |
| Networking | Ktor Client |
| Crash Reporting | Firebase Crashlytics |
| Analytics | Firebase Analytics |
| App Updates | Google Play In-App Updates |
| Auth | AndroidX Biometric |
| Splash | AndroidX SplashScreen |

---

## 📁 Project Structure

```
app/src/main/java/com/purnendu/contactly/
│
├── alarm/                    # Alarm system
│   ├── AliasAlarmReceiver    # BroadcastReceiver — applies/reverts contact changes
│   ├── RescheduleAlarmsReceiver  # Re-registers alarms on device boot
│   ├── ContactlyAlarmManager # Central alarm scheduling logic
│   ├── AlarmEventBus         # Event bus for real-time UI updates
│   └── models/               # AlarmMetadata, AlarmResult, etc.
│
├── data/
│   ├── local/
│   │   ├── room/             # Room database, DAO, ScheduleEntity
│   │   └── preferences/      # DataStore AppPreferences
│   └── repository/
│       ├── ContactsRepository    # Read/write device contacts
│       └── SchedulesRepository   # CRUD for schedule entities
│
├── di/
│   └── AppModule.kt          # Koin dependency graph
│
├── model/                    # Domain models (Contact, Schedule)
│
├── notification/             # NotificationHelper
│
├── ui/
│   ├── components/           # Shared composables (dialogs, bottom nav, carousel)
│   ├── screens/
│   │   ├── schedule/         # Home screen — schedule list/grid + edit sheet
│   │   ├── setting/          # Settings screen + components
│   │   └── webView/          # Feedback & Privacy Policy WebViews
│   └── theme/                # Material 3 theme, colors, typography
│
├── utils/                    # Helpers (permissions, biometric, animations, image storage)
│
├── ContactlyApplication.kt  # Koin initialization
└── MainActivity.kt           # Single-activity entry point
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Meerkat (2024.3.1) or later
- **JDK 17**
- **Android SDK 36**

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/PurnenduSamanta/Contactly.git
   cd Contactly
   ```

2. **Open in Android Studio**
   - File → Open → select the project root

3. **Firebase Setup**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Download `google-services.json` and place it in `app/`
   - Enable Crashlytics and Analytics

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or simply hit ▶️ **Run** in Android Studio on a device/emulator with **API 24+**.

### Required Permissions

| Permission | Purpose |
|---|---|
| `READ_CONTACTS` | Fetch contact list to create schedules |
| `WRITE_CONTACTS` | Apply temporary name & photo changes |
| `SCHEDULE_EXACT_ALARM` | Precise alarm scheduling |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after device reboot |
| `POST_NOTIFICATIONS` | Show notifications when swaps happen (Android 13+) |

---

## 📲 Download

<a href="https://play.google.com/store/apps/details?id=com.purnendu.contactly">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200" alt="Get it on Google Play"/>
</a>

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is proprietary. All rights reserved © [Purnendu Samanta](https://play.google.com/store/apps/developer?id=Purnendu+Samanta).

---

<p align="center">
  <i>Designed & developed from a true story ❤️</i>
</p>
