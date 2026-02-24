<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="120" alt="Contactly Logo"/>
</p>

<h1 align="center">Contactly</h1>

<p align="center">
  <b>Smart Contact Identity Scheduler for Android</b><br/>
  Temporarily change contact name & photo — instantly or on schedule.
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

**Contactly** gives you full control over your contact identity — temporarily and automatically.

Change a contact’s **name and photo** either:

- ⚡ Instantly (one tap)
- 📅 On a schedule (start & end time)
- 🔁 Repeating weekly on selected days

When the scheduled time ends, everything is restored safely — exactly as it was.

Whether it’s for privacy, fun, organization, or context-based identity switching — Contactly handles it seamlessly in the background.

---

## ✨ Features

### ⚡ Instant Mode
- Apply a temporary identity immediately
- One-tap identity switch
- Perfect for quick changes
- Safe automatic restore

### 📅 Smart Scheduling
- Start & end time support
- One-time schedules
- Weekly repeating schedules (Mon–Sun)
- Exact alarm precision
- Auto re-register after device reboot

### 🎨 Identity Customization
- Temporary name swapping
- Temporary profile photo swapping
- Real-time active state indicator
- Instant restore when deleting a schedule

### 🔐 Security & Privacy
- Biometric App Lock (Fingerprint / Face unlock)
- Optional notifications
- Secure internal storage for temporary images

### 🎨 Modern UI Experience
- Fully redesigned interface
- Material 3 + Dynamic colors
- Light / Dark / System theme
- List & Grid view toggle
- Smooth animations & micro-interactions
- Sliding image carousel preview
- Custom FAB-centered bottom navigation

### 🛡 Reliability & Stability
- Boot-safe alarms
- Uses `setExactAndAllowWhileIdle`
- Real-time UI refresh via internal event bus
- Multiple edge-case bug fixes
- Safe restore logic even after force-stop or reboot

---

## 📸 Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS1.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS2.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS3.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS4.png" width="220"/>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS5.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS6.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS7.png" width="220"/>
  <img src="https://raw.githubusercontent.com/PurnenduSamanta/Contactly/master/app/App_Essentials/PlayStoreScreenShots/SS8.png" width="220"/>
</p>

---

## 🏗 Architecture

Contactly follows a clean, layered architecture with separation of concerns:

```
UI Layer (Compose)
   ↓
ViewModels (StateFlow)
   ↓
Domain / Alarm Manager / Image Storage
   ↓
Repositories (Contacts + Schedules)
   ↓
Room Database + DataStore
   ↓
Platform (AlarmManager + ContentResolver)
```

### Key Decisions

| Area | Approach |
|------|----------|
| UI | 100% Jetpack Compose (Material 3) |
| State | StateFlow + collectAsStateWithLifecycle |
| DI | Koin |
| Database | Room |
| Preferences | DataStore |
| Alarm System | AlarmManager + BroadcastReceiver |
| Contacts API | ContactsContract via ContentResolver |
| Image Loading | Coil |
| Security | AndroidX Biometric |
| Updates | Google Play In-App Updates |
| Crash Reporting | Firebase Crashlytics |

---

## 📁 Project Structure

```
app/src/main/java/com/purnendu/contactly/

├── alarm/
├── data/
│   ├── local/
│   └── repository/
├── di/
├── model/
├── notification/
├── ui/
│   ├── components/
│   ├── screens/
│   └── theme/
├── utils/
├── ContactlyApplication.kt
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Meerkat (2024.3.1) or later
- JDK 17
- Android SDK 36

### Setup

```bash
git clone https://github.com/PurnenduSamanta/Contactly.git
cd Contactly
```

Open in Android Studio and run on an emulator or device (API 24+).

### Firebase Setup
- Create project at console.firebase.google.com
- Download `google-services.json`
- Place inside `app/`
- Enable Crashlytics & Analytics

---

## 🔐 Required Permissions

| Permission | Purpose |
|------------|----------|
| READ_CONTACTS | Read contact data |
| WRITE_CONTACTS | Modify name & photo |
| SCHEDULE_EXACT_ALARM | Precise scheduling |
| RECEIVE_BOOT_COMPLETED | Re-register alarms |
| POST_NOTIFICATIONS | Show swap notifications |

---

## 📲 Download

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.purnendu.contactly">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200"/>
  </a>
</p>

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repo
2. Create feature branch
3. Commit changes
4. Open Pull Request

---

## 📄 License

Proprietary — All rights reserved © Purnendu Samanta

---

<p align="center">
  <i>Designed & developed from a true story ❤️</i>
</p>
