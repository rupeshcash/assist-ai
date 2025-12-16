# AssistEyes - Setup Instructions

## Phase 1 Implementation Complete! 🎉

You now have a working end-to-end AI vision assistant that can:
- Listen to voice questions via microphone
- Capture camera frames
- Send to Gemini AI for analysis
- Speak back the response

---

## Setup Steps

### 1. Get Your Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the API key

### 2. Add API Key to the App

**Option A: Direct in Code (Quick Test)**
1. Open `/app/src/main/java/com/usbcamera/CameraPreviewActivity.java`
2. Find line 20: `private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE";`
3. Replace `YOUR_API_KEY_HERE` with your actual API key
4. Example: `private static final String GEMINI_API_KEY = "AIzaSyD...your_key_here";`

**Option B: Using local.properties (Recommended for Security)**
1. Open or create `/local.properties` file in project root
2. Add this line:
   ```
   GEMINI_API_KEY=AIzaSyD...your_key_here
   ```
3. Update `build.gradle` to read it (we'll do this later if needed)

### 3. Sync Gradle

1. In Android Studio, click **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. Make sure no errors appear

### 4. Build and Run

1. Connect your Android device via USB
2. Enable USB debugging on your device
3. Click the **Run** button in Android Studio
4. Select your device
5. Wait for the app to install and launch

---

## How to Use

### First Time Setup:
1. **Connect USB Camera** via OTG adapter to your phone
2. **Grant USB permission** when prompted
3. **Grant microphone permission** when prompted
4. The camera preview should appear

### Using the Voice Assistant:
1. **Tap the blue microphone button** (floating button in center)
2. Button turns **red** - you're now listening
3. **Speak your question**:
   - "What do you see?"
   - "Describe what's in front of me"
   - "Are there any obstacles?"
   - "What objects are on the table?"
   - "Read any text you see"
4. Wait for processing (status will show "Processing...")
5. **Listen to the AI response** (spoken aloud)
6. Button turns **blue** again - ready for next question

---

## Troubleshooting

### "API Key Error"
- Make sure you replaced `YOUR_API_KEY_HERE` with your actual Gemini API key
- Check that the API key is valid at Google AI Studio

### "Microphone Permission Denied"
- Go to Settings → Apps → AssistEyes → Permissions
- Enable Microphone permission

### "Camera not working"
- Make sure USB camera is connected via OTG
- Try unplugging and replugging the camera
- Go back to MainActivity and reconnect

### "No internet connection"
- Gemini requires internet to work
- Make sure your device has WiFi or mobile data
- Check that INTERNET permission is granted

### "Couldn't hear you"
- Speak clearly after the mic turns red
- Make sure you're not in a very noisy environment
- Check microphone permissions

---

## Testing Scenarios

Try these preset questions to test the app:

### Basic Description:
- "What do you see?"
- "Describe what's in front of me"

### Obstacle Detection:
- "Are there any obstacles?"
- "Is the path clear?"
- "What's ahead of me?"

### Object Identification:
- "What objects are here?"
- "Which one is the salt shaker?"
- "What's on the table?"

### Text Reading:
- "Read the text"
- "What does the label say?"

---

## Architecture

```
User Taps Mic Button
        ↓
VoiceManager starts listening
        ↓
User speaks question
        ↓
Speech-to-Text converts to text
        ↓
CameraPreviewFragment captures current frame
        ↓
GeminiClient sends frame + query to Gemini API
        ↓
Gemini analyzes and responds
        ↓
VoiceManager speaks response via TTS
        ↓
Ready for next question
```

---

## API Costs

**Gemini Flash Pricing:**
- **Free Tier:** 60 requests per minute
- **Cost:** ~$0.01 per request (with image)
- **For Demo:** Practically free for testing!

---

## Next Steps (Phase 2)

After Phase 1 is working, we can add:
- TensorFlow Lite for instant offline obstacle detection
- Preset prompts (buttons for common questions)
- Continuous monitoring mode
- Better UI with animations
- Settings screen

---

**Ready to test! 🚀**

Build the app, connect your camera, and start asking questions!
