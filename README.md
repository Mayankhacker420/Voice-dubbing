# Hindi Dubber

**Live AI Voice Dubbing for Android**

Captures audio from any app, translates it in real-time, and speaks it in hi.

## Features

- Universal audio capture (YouTube, Netflix, Instagram, etc.)
- Real-time translation via LibreTranslate (free, no API key needed)
- Speech-to-text via Groq Whisper (free tier, generous quota)
- Android Neural TTS output
- Floating draggable overlay bubble
- Background foreground service
- Supports Android 10+ (API 29+)

## How to Build the APK

**Requirements:** Java 17+ installed

### Linux / macOS
```bash
chmod +x BUILD.sh
./BUILD.sh
```

### Windows
```
BUILD.bat
```

The APK will be at: `app/build/outputs/apk/release/`

## Setup on Your Phone

1. Enable **Install from Unknown Sources** in Settings > Security
2. Transfer the APK to your phone and install it
3. Open the app and grant all permissions when prompted
4. Tap **Start Dubbing** and allow Media Projection
5. The floating bubble will appear — open any video app and enjoy!

## Optional: Better STT (Groq Whisper)

For best speech recognition, get a free Groq API key:
1. Sign up free at https://console.groq.com
2. Create an API key
3. Open `app/src/main/java//pipeline/RecognitionPipeline.kt`
4. Replace `YOUR_GROQ_API_KEY_HERE` with your key
5. Rebuild with `./BUILD.sh`

## Configuration

- Target Language: **hi** (hi-IN)
- Source Language: **auto**
- Package: 

## Supported Languages

English, Hindi, Japanese, Spanish, Arabic, French, German, Portuguese, Korean, Chinese

## Architecture

- MediaProjection + AudioPlaybackCaptureAPI (internal audio)
- Groq Whisper API (speech-to-text, free tier)
- LibreTranslate community instances (translation, no key needed)
- Android TextToSpeech (voice output)
- Floating overlay via WindowManager
- LifecycleService for background processing
