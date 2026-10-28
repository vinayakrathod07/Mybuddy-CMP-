@echo off
echo ==========================================================
echo 📱 AcademiaLife - USB Mobile App Runner
echo ==========================================================
echo.

where adb >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: 'adb' command not found on your system.
    echo 👉 Please ensure Android SDK Platform-Tools is installed and in your PATH.
    pause
    exit /b 1
)

echo 🔍 Detecting connected Android devices...
adb devices

echo.
echo 🚀 Building and installing debug APK to your phone...
cd /d "%~dp0android-app"
call gradlew.bat installDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo 📱 Launching AcademiaLife on your phone...
    adb shell am start -n com.academialife.app/.MainActivity
    echo.
    echo 🎉 App is now running on your mobile device!
) else (
    echo.
    echo ❌ Build or installation failed. Please check Android Studio or Gradle error messages above.
)

pause
