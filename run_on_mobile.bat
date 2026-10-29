@echo off
setlocal enabledelayedexpansion
echo ==========================================================
echo 📱 AcademiaLife - USB Mobile App Runner
echo ==========================================================
echo.

:: Detect adb
where adb >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%PATH%"
        echo ℹ️ Added Android SDK platform-tools to session PATH.
    ) else (
        echo ❌ Error: 'adb' command not found on your system.
        echo 👉 Please ensure Android SDK Platform-Tools is installed and in your PATH.
        pause
        exit /b 1
    )
)

:: Detect Java JDK if not set
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Android\Android Studio1\jbr" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio1\jbr"
    ) else if exist "C:\Program Files\Android\Android Studio\jbr" (
        set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    )
)

echo 🔍 Detecting connected Android devices...
adb devices

echo.
echo 🚀 Installing debug APK to your device...
cd /d "%~dp0android-app"

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 📦 Found pre-built debug APK. Installing directly...
    adb install -r "app\build\outputs\apk\debug\app-debug.apk"
) else (
    call gradlew.bat installDebug
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo 📱 Launching AcademiaLife on your phone...
    adb shell am start -n com.academialife.app/.MainActivity
    echo.
    echo 🎉 App is now running on your mobile device!
) else (
    echo.
    echo ❌ Installation failed. Please check device connection or USB debugging permission on your phone.
)

pause
