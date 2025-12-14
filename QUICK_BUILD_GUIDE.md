# QUICK BUILD GUIDE - Get Your APK Fast!

## 🚀 OPTION 1: Android Studio (EASIEST - Recommended)

**Time: ~10 minutes (first time)**

### Step 1: Download Android Studio
- Go to: https://developer.android.com/studio
- Download and install (it includes everything needed)

### Step 2: Open Project
1. Launch Android Studio
2. Click **"Open"**
3. Navigate to: `C:\Users\rupes\Downloads\assist-ai`
4. Click **OK**

### Step 3: Wait for Sync
- First time: Android Studio will download dependencies (~5 minutes)
- You'll see "Gradle sync" at the bottom - wait for it to finish

### Step 4: Build APK
1. Click **Build** menu → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait 1-2 minutes
3. Click **"locate"** in the popup notification

**APK Location:**
```
C:\Users\rupes\Downloads\assist-ai\app\build\outputs\apk\debug\app-debug.apk
```

---

## ⚡ OPTION 2: Online Build Service (NO INSTALLATION)

Use AppGyver or similar online Android build services:

1. **Appetize.io** - Upload project, build online
2. **CircleCI** - Free Android builds
3. **GitHub Actions** - Automated builds

---

## 🛠️ OPTION 3: Command Line (For Developers)

### Requirements:
- Java JDK 17 or higher
- Android SDK

### If You Have Requirements:

1. **Set Environment Variables:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=C:\Users\rupes\AppData\Local\Android\Sdk
```

2. **Run Build:**
```cmd
cd C:\Users\rupes\Downloads\assist-ai
gradlew.bat assembleDebug
```

3. **Find APK:**
```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 📱 AFTER YOU HAVE THE APK

### Install on Your Phone:

1. **Transfer APK** to your Android phone (USB, email, cloud, etc.)

2. **Enable Unknown Sources:**
   - Go to Settings → Security
   - Enable "Unknown Sources" or "Install Unknown Apps"

3. **Install:**
   - Tap the APK file on your phone
   - Click "Install"

4. **Connect USB Camera:**
   - Connect USB camera via OTG adapter
   - Open the app
   - Grant USB permissions when prompted

---

## ❓ TROUBLESHOOTING

### Build Fails in Android Studio?
- Make sure you have stable internet connection
- Let Gradle sync complete fully
- Try: **File → Invalidate Caches / Restart**

### Can't find the APK?
Look in: `app\build\outputs\apk\debug\app-debug.apk`

### App won't install on phone?
- Enable "Unknown Sources" in Security settings
- Make sure you have enough storage space

---

## 💡 MY RECOMMENDATION

**Use Android Studio** - it handles all dependencies automatically and is the most reliable method. The initial download is ~1GB, but it's a one-time setup.

---

## ⏱️ Time Estimates

| Method | Time | Difficulty |
|--------|------|-----------|
| Android Studio | ~15 min | Easy |
| Command Line | ~5 min | Medium |
| Online Service | ~10 min | Easy |

---

Need help with any of these options? Let me know!
