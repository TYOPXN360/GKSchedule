#!/bin/bash
# 课程表 App 构建脚本
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME="$SCRIPT_DIR/source/jdk-17"
export ANDROID_HOME="$SCRIPT_DIR/source/android-sdk"
export ANDROID_USER_HOME="$SCRIPT_DIR/.android"
export GRADLE_USER_HOME="$SCRIPT_DIR/.gradle-home"
export HOME="$SCRIPT_DIR/.home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/36.0.0:$PATH"

mkdir -p "$ANDROID_USER_HOME/cache" "$HOME"

echo "🔨 Building ClassApp..."
MODE="${1:-debug}"; shift 2>/dev/null || true
if [ "$MODE" = "release" ]; then
    ./gradlew assembleRelease "$@"
    APK="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
else
    ./gradlew assembleDebug "$@"
    APK="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
fi
if [ -f "$APK" ]; then
    echo ""
    echo "✅ Build successful!"
    echo "📦 APK: $APK ($(du -h "$APK" | cut -f1))"
    echo ""
    # Try to install if device connected
    DEVICE=$(adb devices | awk '$2=="device"{print $1; exit}')
    if [ -n "$DEVICE" ]; then
        echo "📱 Installing on device ($DEVICE)..."
        adb -s "$DEVICE" install -r "$APK"
        echo "✅ Installed!"
        LOCAL_MD5=$(md5sum "$APK" | cut -d' ' -f1)
        REMOTE_APK=$(adb -s "$DEVICE" shell 'pm path com.ty.gkschedule' | tr -d '\r' | cut -d: -f2)
        REMOTE_MD5=$(adb -s "$DEVICE" shell "md5sum $REMOTE_APK" | cut -d' ' -f1)
        if [ "$LOCAL_MD5" = "$REMOTE_MD5" ]; then
            echo "✅ md5一致 ($LOCAL_MD5)，落地确认"
        else
            echo "⚠️ md5不一致 local=$LOCAL_MD5 remote=$REMOTE_MD5"
        fi
    else
        echo "📱 No device connected. Connect a device and run:"
        echo "   adb install -r $APK"
    fi
else
    echo "❌ Build failed - APK not found"
    exit 1
fi
