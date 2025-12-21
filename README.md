# AssistEyes

**AI-powered vision assistant for the visually impaired using affordable USB cameras and Gemini AI.**

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Status](https://img.shields.io/badge/Status-Demo-orange.svg)

---

## Why AssistEyes?

Traditional assistive devices for the visually impaired are expensive and limited:
- Smart canes: ₹10,000+ ($120+)
- Limited functionality (basic obstacle detection only)
- No contextual awareness or AI assistance

**AssistEyes** changes this by using:
- **Affordable USB cameras**: ₹1,000 (~$12) via OTG connection
- **Powerful AI**: Google Gemini 2.5 Flash for intelligent scene understanding
- **Natural interaction**: Voice-based questions and answers

**Result**: 10x cheaper, infinitely smarter assistance.

---

## What It Does

### Core Features

#### 1. **Voice-Activated Questions**
- User taps mic button and asks questions naturally
- AI analyzes camera view and responds with voice
- Examples:
  - "What's in front of me?"
  - "Where is the kitchen?"
  - "Which bottle is the milk?"
  - "Is there a chair nearby?"

#### 2. **Active Mode (Continuous Monitoring)**
- Automatically scans environment every 3 seconds
- Proactive alerts for obstacles and hazards
- Customizable instructions via settings
- Example: "Alert me about obstacles at head level"

#### 3. **Smart AI Processing**
- Gemini 2.5 Flash Lite for speed
- Image optimization (512px) for fast uploads
- Concise responses (1-2 sentences max)
- Context-aware prompting

---

## How It Works

### Architecture

```
┌─────────────────────────────────────┐
│   USB Camera (OTG Connected)        │
│   Mounted on glasses/head           │
└──────────────┬──────────────────────┘
               │ Video Feed (1080p)
               ▼
┌─────────────────────────────────────┐
│   Android App (AssistEyes)          │
│                                     │
│  1. Capture frame from camera       │
│  2. Optimize image (resize to 512px)│
│  3. Send to Gemini API with prompt  │
│  4. Receive AI response             │
│  5. Convert to speech (TTS)         │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Gemini 2.5 Flash API              │
│   Scene understanding + reasoning   │
└─────────────────────────────────────┘
```

### Technical Flow

1. **Input**: User taps mic → Android SpeechRecognizer converts speech to text
2. **Capture**: App grabs current frame from USB camera preview
3. **Optimize**: Image resized to 512px (reduces size by ~75%)
4. **AI Processing**: Frame + query sent to Gemini 2.5 Flash
5. **Response**: AI analyzes scene and generates concise answer
6. **Output**: Android TTS speaks response to user

**Total latency**: 2-4 seconds (down from 8-10s with optimization)

---

## Tech Stack

### Core Technologies
- **Platform**: Android (Java)
- **USB Camera**: [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera) (jiangdongguo/libausbc 3.3.6)
- **AI Model**: Google Gemini 2.5 Flash Lite
- **Voice**: Android SpeechRecognizer + TextToSpeech

### Key Libraries
- `com.google.ai.client.generativeai:generativeai:0.9.0` - Gemini SDK
- `com.google.guava:guava:31.1-android` - Futures/async handling
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` - Coroutines
- Material Design Components 1.10.0

### Performance Optimizations
- Image compression: 1920x1080 → 512x512 (~75% size reduction)
- Model: Gemini 2.5 Flash Lite (1.5x faster than 2.0 Flash)
- Async processing: Non-blocking UI with Executor pattern
- Smart caching: SharedPreferences for custom instructions

---

## Setup

### Prerequisites
- Android device with USB OTG support
- USB camera (any UVC-compatible webcam)
- OTG adapter
- Google Gemini API key ([Get free key](https://makersuite.google.com/app/apikey))

### Installation

1. **Clone repository**
   ```bash
   git clone https://github.com/rupeshcash/assist-ai.git
   cd assist-ai
   ```

2. **Add Gemini API key**

   Open `local.properties` and add:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Hardware Setup
1. Connect USB camera to phone via OTG adapter
2. Grant USB and microphone permissions when prompted
3. Camera preview should appear automatically

---

## Usage

### Basic Usage

1. **Launch app** → Tap "Start AssistEyes"
2. **Grant permissions** (USB, Microphone)
3. **Tap mic button** (large blue circle)
4. **Ask question** naturally
5. **Listen to response**

### Active Mode

1. Toggle **"Active Mode"** switch
2. Tap **⚙ settings** to customize instructions
3. App monitors environment every 3 seconds
4. Automatic voice alerts for obstacles/hazards

### Custom Instructions Examples
- "Alert me about obstacles at head level"
- "Focus on stairs and steps"
- "Tell me about people nearby"
- "Warn me about low-hanging objects"

---

## Project Evolution

### Phase 1: REST API Implementation ✅
- Single-frame analysis
- Voice input/output
- Active mode with customization
- Performance optimizations

**Timeline**: 5 days
**Status**: Complete and demo-ready

### Phase 2: Live API (Planned)
- Real-time video streaming (1 FPS)
- Bidirectional audio/video
- Sub-second latency
- Persistent WebSocket connection

**Timeline**: 3-5 days
**Status**: Planned (see `LIVE_API_PLAN.md`)

---

## Performance Benchmarks

| Metric | Before Optimization | After Optimization |
|--------|--------------------|--------------------|
| Image size | 1920x1080 | 512x512 |
| Upload size | ~400KB | ~60KB |
| Total latency | 8-10 seconds | 2-4 seconds |
| Model | gemini-1.5-flash | gemini-2.5-flash-lite |

**Improvement**: 60-70% faster response times

---

## Accessibility Features

### Visual
- **Huge buttons**: Mic button 96dp diameter (2x standard)
- **High contrast**: #CC000000 cards on black background
- **Large text**: 18-22sp (up from 14-16sp)
- **Clear spacing**: 24-32dp margins for easy targeting

### Auditory
- Voice-first interaction
- Text-to-speech for all responses
- Spoken status updates

### Physical
- One-tap interaction (single mic button)
- No complex gestures required
- Can be operated eyes-free

---

## Known Limitations

1. **503 Errors**: Free tier has low rate limits (15 RPM, 20 requests/day)
   - **Solution**: Upgrade to paid tier or use Live API

2. **Internet Required**: Cloud-based AI needs connectivity
   - **Future**: On-device AI with TensorFlow Lite

3. **2-4 Second Latency**: REST API overhead
   - **Solution**: Migrate to Live API for <1s latency

4. **Single Frame Analysis**: No temporal context
   - **Solution**: Live API streaming (Phase 2)

---

## Future Roadmap

- [ ] **Live API Integration** - Real-time streaming, <1s latency
- [ ] **On-device AI** - TensorFlow Lite for offline use
- [ ] **Object Tracking** - Persistent identification across frames
- [ ] **Navigation Mode** - Turn-by-turn guidance
- [ ] **OCR Mode** - Read text from signs, labels, documents
- [ ] **Face Recognition** - Identify familiar people
- [ ] **Multi-language Support** - Beyond English

---

## Demo Showcase

**Use Cases Demonstrated:**
1. Kitchen navigation ("Where is the salt?")
2. Obstacle detection ("What's in front of me?")
3. Object identification ("Which bottle is milk?")
4. Scene description ("Describe my surroundings")
5. Text reading ("Read the label on this bottle")

**Target Demo Time**: 2-3 minutes
**Wow Factor**: Real-time AI assistance at 1/10th the cost of alternatives

---

## Contributing

This is a demo project for showcasing affordable AI-powered assistive technology. Contributions welcome!

**Areas for contribution:**
- Live API implementation
- On-device model integration
- UI/UX improvements
- Performance optimizations
- Multi-language support

---

## License

MIT License - See [LICENSE](LICENSE) file

---

## Acknowledgments

- **Google Gemini AI** - Powerful vision model
- **jiangdongguo** - [AndroidUSBCamera library](https://github.com/jiangdongguo/AndroidUSBCamera)
- **Claude Sonnet 4.5** - Development assistance via [Claude Code](https://claude.com/claude-code)

---

## Contact

**Developer**: Rupesh Kashyap
**Repository**: [github.com/rupeshcash/assist-ai](https://github.com/rupeshcash/assist-ai)
**Demo**: Coming soon

---

**Built with ❤️ to make AI-powered vision assistance accessible to everyone.**