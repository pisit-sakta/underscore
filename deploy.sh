#!/bin/bash
# deploy.sh — Build and install Underscore on connected Android device
# Usage: ./deploy.sh

set -e

echo "_ UNDERSCORE — deploy"
echo ""

# Detect Java
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -d "/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot" ]; then
    export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Detect Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/android-sdk" ]; then
        export ANDROID_HOME="$HOME/android-sdk"
    elif [ -d "$LOCALAPPDATA/Android/Sdk" ]; then
        export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
    fi
fi

echo "[1/3] Building..."
./gradlew assembleDebug -q

APK="app/build/outputs/apk/debug/app-debug.apk"
SIZE=$(du -h "$APK" | cut -f1)
echo "[2/3] Built: $APK ($SIZE)"

# Check for ADB
ADB="adb"
if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb.exe" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb.exe"
fi

# Check for connected device
if $ADB devices 2>/dev/null | grep -q "device$"; then
    echo "[3/3] Installing on device..."
    $ADB install -r "$APK"
    echo ""
    echo "Installed. Launching..."
    $ADB shell am start -n com.underscore.app/.MainActivity
    echo ""
    echo "_ UNDERSCORE is running on your device."
else
    echo "[3/3] No device connected via USB."
    echo "      APK ready at: $APK"
    echo "      Connect your phone with USB debugging enabled, then re-run."
fi
