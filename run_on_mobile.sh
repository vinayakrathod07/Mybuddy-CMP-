#!/bin/bash
set -e

echo "=========================================================="
echo "📱 AcademiaLife - USB Mobile App Runner"
echo "=========================================================="
echo ""

# Check for adb
if ! command -v adb &> /dev/null; then
    echo "❌ Error: 'adb' command not found on your system."
    echo "👉 Please ensure Android SDK Platform-Tools is installed and in your PATH."
    exit 1
fi

echo "🔍 Detecting connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" || true)

if [ -z "$DEVICES" ]; then
    echo "⚠️  No authorized device found."
    echo "👉 Check:"
    echo "   1. Is your phone connected via USB cable?"
    echo "   2. Is 'Developer Options' -> 'USB Debugging' enabled in your phone Settings?"
    echo "   3. Did you unlock your phone and tap 'Allow USB debugging'?"
    adb devices
    exit 1
fi

echo "✅ Connected device detected:"
adb devices

echo ""
echo "🚀 Building and installing debug APK to your phone..."
cd "$(dirname "$0")/android-app"
./gradlew installDebug

echo ""
echo "📱 Launching AcademiaLife on your phone..."
adb shell am start -n com.academialife.app/.MainActivity

echo ""
echo "🎉 App is now running on your mobile device!"
