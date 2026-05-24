@echo off
echo =================================================
echo   Hindi Dubber - Auto Build Script (Windows)
echo =================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java is not installed.
    echo Install Java 17 from: https://adoptium.net
    pause
    exit /b 1
)

echo [1/4] Generating keystore...
if not exist "app\keystore.jks" (
    keytool -genkey -v -keystore app\keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dubberkey -storepass dubber123 -keypass dubber123 -dname "CN=Dubber, OU=App, O=Dubber, L=City, S=State, C=IN"
)

echo [2/4] Building release APK...
call gradlew.bat assembleRelease --no-daemon

echo [3/4] Done!
for /r app\build\outputs\apk\release %%f in (*.apk) do (
    echo APK: %%f
)

echo.
echo Install on your Android phone via USB or ADB.
pause
