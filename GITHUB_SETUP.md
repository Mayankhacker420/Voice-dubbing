# How to Get Your APK — Free Auto Build via GitHub

No Android Studio. No Java. No setup on your computer.
GitHub builds the APK for you in the cloud — takes about 5 minutes.

---

## Step-by-Step Guide

### Step 1 — Create a free GitHub account
Go to https://github.com and sign up (free).

### Step 2 — Create a new repository
1. Click the **+** button (top right) → "New repository"
2. Name it: `hindi-dubber`
3. Set to **Public** (required for free GitHub Actions)
4. Click **Create repository**

### Step 3 — Upload your project files
**Option A — GitHub website (easiest):**
1. Extract this ZIP on your computer
2. Go to your new repository on GitHub
3. Click **"uploading an existing file"**
4. Drag the entire `Hindi_Dubber/` folder into the upload area
5. Click **"Commit changes"**

**Option B — Git (if you have it installed):**
```bash
cd Hindi_Dubber
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/hindi-dubber.git
git push -u origin main
```

### Step 4 — Wait for GitHub to build your APK (~5 minutes)
1. Go to your repository on GitHub
2. Click the **"Actions"** tab
3. You'll see **"Build Release APK"** running
4. Wait for the green checkmark

### Step 5 — Download your APK
**Option A — From Releases (easier):**
1. Go to your repository → click **"Releases"** (right side)
2. Download the `.apk` file

**Option B — From Actions:**
1. Click on the completed Actions run
2. Scroll to **Artifacts** at the bottom
3. Download **Hindi-Dubber-release-apk**

### Step 6 — Install on your Android phone
1. Transfer the APK to your phone (USB, Google Drive, WhatsApp, etc.)
2. On your phone: **Settings > Security > Install from Unknown Sources** (enable it)
3. Open the APK file and tap Install
4. Open **Hindi Dubber**, grant all permissions
5. Tap **Start Dubbing**, allow Media Projection
6. Open YouTube — the floating bubble appears!

---

## Troubleshooting

**"Actions failed" error?**
- Make sure you uploaded ALL files including the `.github/` folder
- The `.github` folder may be hidden on Mac — press Cmd+Shift+. to show hidden files

**APK won't install?**
- Enable "Install from Unknown Sources" in Settings > Security
- On some phones: Settings > Apps > Special App Access > Install Unknown Apps

**No Hindi voice on YouTube?**
- The app translates and speaks over the original audio
- Make sure your phone volume is up
- If no voice: Settings > Accessibility > Text-to-Speech > install Google Hindi voice pack

---

## Requirements for the App to Work

- Android 10 or higher (API 29+)
- Internet connection (for translation API)
- Overlay permission (for floating bubble)
- Media Projection permission (to capture YouTube audio)

---

*Hindi Dubber — Built with AI Dubber OS*
