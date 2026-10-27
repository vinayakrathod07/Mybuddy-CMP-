# AcademiaLife - Native Android Application (Kotlin + XML)

A native Android Studio application converted from the Student Academic Assistant App, featuring Firebase Firestore persistence, Auth, real-time timetable collision detection, deterministic task priority scoring, attendance compliance calculation, marks analytics, and voice-enabled AI tutor assistant.

---

## 🚀 How to Run in Android Studio

1. **Open Android Studio**.
2. Click **File -> Open...** (or **Open** from the welcome screen).
3. Select the `android-app` folder from this project directory.
4. Android Studio will automatically recognize the Gradle build files (`settings.gradle.kts`, `build.gradle.kts`, and `gradle/libs.versions.toml`).
5. Wait for **Gradle Sync** to finish.
6. Connect an Android device with USB debugging enabled or start an Android Emulator (API 24+ recommended, e.g., Android 12, 13, 14).
7. Click the green **Run** (▶) button or press `Shift + F10`.

---

## 📁 Project Architecture & Components

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Configured with INTERNET and RECORD_AUDIO permissions
│   │   ├── google-services.json         # Firebase project configuration
│   │   ├── java/com/academialife/app/
│   │   │   ├── MainActivity.kt          # Host Activity with MaterialToolbar & BottomNavigationView
│   │   │   ├── Models.kt                # Data models matching Firestore schemas (zero sample data)
│   │   │   ├── AcademicCalculators.kt   # Deterministic priority scoring, attendance calculators & collision checks
│   │   │   ├── FirebaseRepository.kt    # Firestore CRUD with custom databaseId & Auth
│   │   │   ├── DashboardFragment.kt     # Metric cards, quick attendance logger, upcoming tasks
│   │   │   ├── TimetableFragment.kt     # Weekly timetable with conflict detection & dialog
│   │   │   ├── TimetableAdapter.kt      # RecyclerView adapter for timetable cards
│   │   │   ├── TasksFragment.kt         # Assignment & Exam tracker with priority badge
│   │   │   ├── TasksAdapter.kt          # RecyclerView adapter for tasks with status toggle
│   │   │   ├── AttendanceFragment.kt    # Attendance compliance ledger with target calculations
│   │   │   ├── AttendanceAdapter.kt     # RecyclerView adapter for attendance subjects
│   │   │   ├── MarksAnalyticsFragment.kt# Assessments and grade percentage analytics
│   │   │   ├── MarksAdapter.kt          # RecyclerView adapter for assessments
│   │   │   ├── AIChatFragment.kt        # AI Assistant with SpeechRecognizer and TextToSpeech
│   │   │   └── ChatAdapter.kt           # RecyclerView adapter for chat bubbles with TTS
│   │   └── res/
│   │       ├── layout/                  # Responsive XML layouts matching Material 3
│   │       ├── menu/                    # bottom_nav_menu.xml & top_app_bar_menu.xml
│   │       ├── values/                  # colors.xml, strings.xml, themes.xml
│   │       └── xml/                     # backup_rules.xml, data_extraction_rules.xml
│   ├── build.gradle.kts                 # App module Gradle configuration
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml               # Version catalog for AGP 8.3+, Kotlin, Material, Firebase
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts                     # Root build configuration
└── settings.gradle.kts
```

---

## ⚡ Key Features

1. **No Sample Data:** Starts clean; records are created when the student adds courses, classes, tasks, or marks.
2. **Firebase Firestore Database:** Connected to database `ai-studio-academialifestud-faab33c7-9821-445b-9c14-7c8200013f6a`.
3. **Deterministic Task Priority:** Computes priority score based on type weight (exam: 40, project: 30, assignment: 20, revision: 15), urgency based on due date proximity, and effort.
4. **Attendance Compliance:** Mathematically computes classes needed to meet target percentage (e.g. 75%) or safe margin of classes that can be missed.
5. **Timetable Collision Detection:** Flags overlapping time intervals when scheduling classes on the same day.
6. **Voice AI Assistant:** Integrated with Android `SpeechRecognizer` for voice prompts and `TextToSpeech` for read-aloud responses.
