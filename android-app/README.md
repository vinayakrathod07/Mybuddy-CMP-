# ⚡ MyBuddy - Vibrant Student Life & Academic Companion

A high-performance native Android application transformed with the **Dopamine Rush** design system and comprehensive student life management features:

- 🎨 **Dopamine Rush Visual Design System:** Electric Violet (`#7952F2`), Hot Coral (`#FF6B5E`), Sunshine Yellow (`#FFD84D`), Acid Lime (`#D6F56A`), Cobalt Blue (`#4169F5`), Soft Pink (`#FF9FCB`), and Day/Night Dark Mode.
- 🚀 **Dynamic Home Dashboard:** Time-based personalized greeting, study streak counter, XP points & level badge, animated daily productivity score, urgent deadlines, and one-tap hydration tracker.
- 🤖 **Multilingual AI Tutor ("Dost"):** Centered on the bottom navigation bar, supporting 8 languages (English, Hindi, Hinglish, Kannada, Telugu, Tamil, Marathi, Spanish) with Google ML Kit AI Vision text scanning for timetable photos and syllabus notes.
- 💰 **Student Finance & Budget Wallet:** Monthly budget manager with real-time remaining calculations, categorized expense logging (Food, Travel, Books, Shopping), and XP rewards (+20 XP).
- 🌿 **Student Wellness & Daily Habits:** Daily mood check-in (5 mood states), 8-glass daily water hydration tracker, sleep tracker, and daily journal reflection (+30 XP).
- 🏆 **Gamification & Badges:** XP points system, level progression (Freshman to Academic Titan), study streaks, and genuine activity-based achievement badges (Focus Master, Streak Champion, Assignment Crusher).
- 🗓️ **Timetable Schedule with AI Vision:** Weekly lectures & labs manager with AI timetable photo auto-import and schedule collision warnings.
- 📈 **Attendance Compliance Ledger:** Precise mathematical safety margin calculations, safe skips, and consecutive classes required to maintain target percentage.
- ⏱️ **Study Zone & Pomodoro Timer:** 25/5 and 50/10 focus presets, break timers, focus session history, and smart routine auto-generation.
- 📚 **Smart Notes & Course Document Vault:** Categorized academic storage for lecture notes, question papers, and cheatsheets.
- 🌓 **Complete Theme Manager:** Seamless Light Mode, Dark Mode (`#171421`), and System theme switcher with persistent preferences.

---

## 🚀 How to Run

### Option 1: One-Click Runner (USB Connected Phone)
1. Enable **USB Debugging** on your Android device (Settings -> Developer Options -> USB Debugging).
2. Connect your phone to your PC via USB cable.
3. Double-click `run_on_mobile.bat` in the project root.
   - It will automatically detect `adb`, install the pre-built APK, and launch MyBuddy!

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

## 📂 Project Architecture

```
android-app/app/src/main/java/com/academialife/app/
├── MainActivity.kt               # Navigation host with 5 bottom tabs: Home, Planner, Dost AI, Timetable, Life
├── Models.kt                     # Data models (Gamification, Wallet, Wellness, Notes, Timetable, Tasks, Attendance)
├── AIVisionAnalyzer.kt           # Google ML Kit Text Recognition for timetable & attendance photo scanning
├── ThemeManager.kt               # Dynamic Day/Night Dark Mode manager and SharedPreferences persistence
├── FirebaseRepository.kt         # Firestore CRUD, Multilingual AI RAG Engine, Gamification XP, Finance & Wellness
├── DashboardFragment.kt          # Dopamine hero gradient card, productivity score, module grid, hydration widget
├── LifestyleFragment.kt          # Student Life Zone (Wallet Budget, Wellness Habits, Badges & Achievements)
├── AIChatFragment.kt             # Multilingual AI Tutor "Dost" with voice TTS/STT and camera photo scanner
├── TimetableFragment.kt          # Schedule manager with AI timetable photo scanner and collision detection
├── TasksFragment.kt              # Assignment & exam planner with priority scoring and completion XP (+50 XP)
├── AttendanceFragment.kt         # Attendance compliance ledger with safe skips and AI sheet scanner
├── StudyPlannerFragment.kt       # Pomodoro timer (25/5, 50/10), cycle tracker, and study routine generator
├── MarksAnalyticsFragment.kt     # GPA/Average analytics, top-scored subjects, and assessment records
├── DocumentVaultFragment.kt      # Course document repository (Lecture Notes, Syllabus, Cheatsheets)
├── ProfileFragment.kt            # Appearance / Dark Mode selector, academic targets, and account management
├── LoginActivity.kt              # Dopamine-themed authentication with Google Sign-In and Guest Mode
```

---

## 👨‍💻 Credits & Attribution

**Developed and Managed by Vinay Rathod**
