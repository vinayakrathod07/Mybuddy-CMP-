# 🎓 AcademiaLife (MyBuddy) - Native Neumorphic Android Application

A high-performance native Android application converted from the Student Academic Assistant web platform, completely redesigned with a **Neumorphic (Soft UI) tactile aesthetic** and packed with full feature parity:

- 🎨 **Neumorphic Design System:** Custom extruded surfaces, dual-shadow ambient highlights, inset debossed text inputs, soft pill chips, and tactile buttons.
- 📊 **Dynamic Dashboard:** Real-time academic metrics, Daily AI Academic Briefing, and One-Tap Attendance Quick-Logger.
- 🗓️ **Timetable & Collision Detection:** Weekly schedule manager with real-time time interval overlap warning.
- ✅ **Deterministic Task Priority:** Urgency algorithms weighing type, proximity, effort, and base weight into a 0–100 score.
- 📈 **Attendance Compliance Ledger:** Exact mathematical calculations determining consecutive classes needed or safe margin before shortage (< 75%).
- ⏱️ **Study Planner & Pomodoro Timer:** 25-minute focus session / 5-minute break countdown timer with cycle counter and AI study plan auto-generator.
- 📚 **Course Document Vault:** Categorized academic repository (Syllabus, Lecture Notes, Past Papers, Cheatsheets).
- 🔔 **Smart Alerts Center:** Real-time warnings for attendance shortages, approaching deadlines, and exam schedules.
- 🔍 **Global Search Hub:** Instant search across subjects, timetable slots, tasks, marks, and documents.
- 🤖 **Voice-Enabled AI Tutor:** Real-time academic RAG assistant with Android `SpeechRecognizer` voice input and `TextToSpeech` audio playback.
- 👤 **Student Profile & Settings:** Custom minimum attendance target (75%, 80%, etc.), study hour preferences, and Firebase Auth / Demo Guest switcher.

---

## 🚀 How to Run

### Option 1: One-Click Runner (USB Connected Phone)
1. Enable **USB Debugging** on your Android device (Settings -> Developer Options -> USB Debugging).
2. Connect your phone to your PC via USB cable.
3. Double-click `run_on_mobile.bat` in the project root.
   - It will automatically detect `adb` from your Android SDK, install the pre-built APK, and launch the app!

### Option 2: Android Studio
1. Open **Android Studio**.
2. Select **File -> Open...** and choose the `android-app` folder.
3. Wait for Gradle Sync to complete.
4. Select your connected device or emulator and press **Run (▶)** (`Shift + F10`).

---

## 📦 Pre-Built APK Location
The compiled debug APK is ready for deployment at:
```
android-app/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎨 Neumorphic Design System Architecture

```
android-app/app/src/main/res/drawable/
├── bg_neumorphic_card.xml          # Extruded dual-shadow surface (20dp radius)
├── bg_neumorphic_card_glow.xml     # Extruded card with vibrant primary accent glow
├── bg_neumorphic_sunken.xml        # Inset debossed container for text fields & search bars
├── bg_neumorphic_btn.xml           # Tactile button state-list (Raised -> Sunken on press)
├── bg_neumorphic_btn_primary.xml   # High-contrast indigo button with soft drop shadow
├── bg_neumorphic_circle_btn.xml    # Circular tactile buttons for FABs, Mic, Timer controls
├── bg_neumorphic_chip.xml          # Pill chip (Normal vs Sunken Selected)
├── bg_neumorphic_top_bar.xml       # Seamless header toolbar with soft drop shadow
├── bg_neumorphic_bottom_nav.xml    # Floating bottom navigation bar with ambient top shadow
└── bg_pill_safe / warning / critical.xml # Semantic status compliance badges
```

---

## 📂 Project Structure

```
android-app/app/src/main/java/com/academialife/app/
├── MainActivity.kt               # Navigation host with Neumorphic Top Bar & Bottom Navigation
├── Models.kt                     # Data models (Profile, Timetable, Task, Attendance, Study, Doc, Notif)
├── AcademicCalculators.kt        # Deterministic scoring, attendance compliance math, conflict detection
├── FirebaseRepository.kt         # Firestore CRUD, Auth, Smart Notifications, Global Search, AI RAG
├── DashboardFragment.kt          # Metric cards, one-tap attendance logger, upcoming schedule & tasks
├── TimetableFragment.kt          # Day-filtered timetable with collision detection dialog
├── TimetableAdapter.kt           # Class cards with room, time badge, faculty, and delete action
├── TasksFragment.kt              # Assignment/exam tracker with filter chips and completion progress bar
├── TasksAdapter.kt               # Task cards with priority score pills and strike-through toggling
├── AttendanceFragment.kt         # Overall compliance ring, target comparison, and subject cards
├── AttendanceAdapter.kt          # Subject cards with [+ Log Present] and [- Log Absent] actions
├── MarksAnalyticsFragment.kt     # GPA/Average analytics, top scored subject, assessment history
├── MarksAdapter.kt               # Assessment cards with percentage badge
├── StudyPlannerFragment.kt       # Pomodoro countdown timer (25m/5m), auto-generator, and session list
├── StudySessionAdapter.kt        # Scheduled study blocks with check-off action
├── DocumentVaultFragment.kt      # Categorized course documents with search and open URL action
├── DocumentAdapter.kt            # Document cards with category pill
├── NotificationsFragment.kt      # Smart alerts for attendance shortage, deadlines, and exams
├── NotificationsAdapter.kt       # Notification cards with alert icons
├── SearchFragment.kt             # Global instant search across all student academic records
├── SearchAdapter.kt              # Categorized search results with tap-to-navigate
├── AIChatFragment.kt             # AI Academic Assistant with SpeechRecognizer and TextToSpeech
└── ChatAdapter.kt                # Neumorphic chat bubbles with read-aloud TTS button
```

---

## 👨‍💻 Credits & Attribution

**Developed and Managed by Vinay Rathod**

