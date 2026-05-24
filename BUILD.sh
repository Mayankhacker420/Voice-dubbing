#!/bin/bash
set -e

echo "================================================="
echo "  Hindi Dubber - Auto Build Script"
echo "  Generates a signed release APK"
echo "================================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed."
    echo "Install Java 17 from: https://adoptium.net"
    exit 1
fi

echo "[1/4] Generating debug keystore..."
if [ ! -f "app/keystore.jks" ]; then
    keytool -genkey -v \
        -keystore app/keystore.jks \
        -keyalg RSA -keysize 2048 \
        -validity 10000 \
        -alias dubberkey \
        -storepass dubber123 \
        -keypass dubber123 \
        -dname "CN=Dubber, OU=App, O=Dubber, L=City, S=State, C=IN" \
        2>/dev/null
    echo "  Keystore created."
else
    echo "  Keystore already exists."
fi

echo "[2/4] Setting permissions..."
chmod +x gradlew

echo "[3/4] Building release APK (this may take 5-10 minutes on first run)..."
./gradlew assembleRelease --no-daemon

echo "[4/4] APK ready!"
echo ""
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
if [ -n "$APK_PATH" ]; then
    echo "  APK Location: $APK_PATH"
    echo ""
    echo "Install on your Android phone:"
    echo "  Option 1: Transfer via USB and install"
    echo "  Option 2: adb install $APK_PATH"
    echo ""
    echo "IMPORTANT: On your phone, enable:"
    echo "  Settings > Security > Install from Unknown Sources"
    echo ""
    echo "================================================="
    echo "  BUILD COMPLETE"
    echo "================================================="
else
    echo "ERROR: APK not found. Check build output above."
    exit 1
fi
