# AssistEyes Phase 2: Gemini Live API Implementation Plan

## Executive Summary

**Objective**: Upgrade AssistEyes from REST API (single frame analysis) to **Gemini Live API** (real-time video streaming) to achieve near-instant responses for blind users.

**Current Issues**:
- 503 "Model Overloaded" errors on free tier
- 5-10 second latency per request
- Free tier severely limited (20 requests/day, 15 RPM)
- Single frame analysis doesn't provide continuous awareness

**Solution**: Migrate to Gemini Live API with bidirectional video/audio streaming

**Expected Result**: <1 second latency, continuous real-time monitoring, better user experience

---

## Research Findings

### Gemini Live API Capabilities

**Key Features**:
- ✅ **Real-time bidirectional streaming**: WebSocket-based persistent connection
- ✅ **Video input support**: Processes continuous video streams at **1 FPS**
- ✅ **Native audio I/O**: Audio input (user speech) + Audio output (AI response)
- ✅ **Low latency**: Optimized for <1 second response time
- ✅ **Multi-modal**: Text, audio, and video input simultaneously
- ✅ **Function calling**: Can trigger app functions in real-time

**Limitations**:
- ⚠️ Video sessions limited to **2 minutes** (audio-only: 15 minutes)
- ⚠️ Video processing at **1 FPS** (not suitable for fast-motion analysis)
- ⚠️ Currently in **developer preview** (API may change)
- ⚠️ Requires **Firebase** setup and integration

**Supported Models**:
- `gemini-2.5-flash-native-audio-preview-12-2025` (Latest, recommended)

**Video Specifications** (from research):
- Processing rate: 1 frame per second (1 FPS)
- Recommended resolution: 768x768 or lower (configurable via `mediaResolution`)
- Format: Base64-encoded JPEG frames sent via WebSocket
- Session limit: 2 minutes for video + audio

**Audio Specifications**:
- Input: 16-bit PCM, 16kHz, mono
- Output: 24kHz sample rate
- Native audio support (no need for separate TTS)

---

## Current Architecture vs. Live API Architecture

### Current (REST API - Phase 1)
```
User → Tap Mic → Speech-to-Text → Capture Frame →
Upload to Gemini REST API → Wait 5-10s →
Get Response → Text-to-Speech → User hears response
```

**Problems**:
- High latency (5-10 seconds)
- Rate limits (15 requests/minute on free tier)
- Single frame = limited context
- No continuous monitoring

### Proposed (Live API - Phase 2)
```
User → Tap to Start → Open WebSocket Connection →
Continuous video stream (1 FPS) + Audio bidirectional streaming →
Real-time AI processing → Instant audio responses (<1s)
User can interrupt and ask questions anytime
```

**Benefits**:
- Sub-second latency
- Continuous awareness (not single frame)
- Natural conversation flow
- Better rate limits for streaming

---

## Implementation Plan

### Phase 2A: Firebase Setup & Live API Integration (Priority)

**Duration**: 1-2 days

**Tasks**:

1. **Firebase Project Setup**
   - Create/connect Firebase project to the app
   - Add Firebase dependencies to build.gradle
   - Configure Firebase authentication (API key management)
   - Add Firebase AI Logic library

2. **Live API Connection Layer**
   - Create `LiveApiClient.java` to manage WebSocket connection
   - Implement connection lifecycle (connect, disconnect, reconnect)
   - Handle session management (2-minute limit awareness)
   - Add error handling and retry logic

3. **Video Frame Streaming**
   - Modify `CameraPreviewFragment` to capture frames at 1 FPS
   - Implement frame-to-base64 encoding
   - Stream video frames via WebSocket
   - Handle resolution optimization (768x768 target)

4. **Audio Streaming Integration**
   - Replace `VoiceManager` with Live API native audio
   - Implement bidirectional audio streaming
   - Remove dependency on Android TTS (use Gemini's native audio)
   - Keep Android SpeechRecognizer for wake-word or manual trigger

5. **UI/UX Updates**
   - Change from "Tap to speak" to "Tap to start/stop streaming"
   - Add session timer (show remaining time of 2-minute window)
   - Add visual indicator for active streaming session
   - Add reconnect button for session renewal

**Dependencies Added**:
```gradle
// Firebase BoM
implementation platform("com.google.firebase:firebase-bom:34.7.0")

// Firebase AI Logic (Live API support)
implementation "com.google.firebase:firebase-ai"

// Keep existing dependencies
// Guava, Coroutines, USB Camera, Material Design
```

**New Files**:
- `LiveApiClient.java` - WebSocket connection manager
- `VideoFrameStreamer.java` - Capture and encode frames at 1 FPS
- `LiveSessionManager.java` - Session lifecycle and timer
- `firebase-config.json` - Firebase configuration

**Modified Files**:
- `CameraPreviewActivity.java` - Replace REST flow with streaming
- `CameraPreviewFragment.java` - Add 1 FPS frame capture
- `build.gradle` - Add Firebase dependencies
- `AndroidManifest.xml` - Add Firebase metadata

---

### Phase 2B: Enhanced Features (Optional)

**Duration**: 1-2 days

**Tasks**:

1. **Function Calling for Actions**
   - Implement function declarations for common actions
   - Example: "Set reminder", "Save location", "Call emergency contact"
   - Use Live API's function calling capability

2. **Context Persistence**
   - Keep conversation context across 2-minute sessions
   - Auto-reconnect with context summary
   - Store recent interaction history

3. **Performance Optimizations**
   - Adaptive frame rate (reduce to 0.5 FPS when idle)
   - Dynamic resolution based on bandwidth
   - Smart compression for cellular networks

4. **Offline Fallback**
   - Detect network issues
   - Fall back to local obstacle detection (without AI)
   - Queue queries for when connection restored

---

## Technical Architecture

### System Components

```
┌─────────────────────────────────────────────────┐
│           AssistEyes Android App                │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────┐        ┌─────────────────┐   │
│  │   USB Camera │───────▶│ VideoFrame      │   │
│  │   (OTG)      │  1 FPS │ Streamer        │   │
│  └──────────────┘        └────────┬────────┘   │
│                                   │             │
│  ┌──────────────┐        ┌───────▼────────┐    │
│  │ Microphone   │───────▶│ LiveApiClient  │    │
│  │ (Voice Input)│  PCM   │ (WebSocket)    │    │
│  └──────────────┘        └───────┬────────┘    │
│                                   │             │
│  ┌──────────────┐        ┌───────▼────────┐    │
│  │   Speaker    │◀───────│ LiveSession    │    │
│  │ (AI Response)│  Audio │ Manager        │    │
│  └──────────────┘        └────────────────┘    │
│                                                 │
└─────────────────────────────────────────────────┘
                    │
                    │ WebSocket (wss://)
                    │ Video (1 FPS, Base64 JPEG)
                    │ Audio (16kHz PCM, bidirectional)
                    ▼
┌─────────────────────────────────────────────────┐
│            Firebase / Google Cloud              │
│         Gemini Live API (Vertex AI)             │
│   gemini-2.5-flash-native-audio-preview         │
└─────────────────────────────────────────────────┘
```

### Data Flow

**Continuous Streaming Mode**:
1. User taps "Start Streaming"
2. App opens WebSocket connection to Gemini Live API
3. Camera captures frames at 1 FPS → Base64 encode → Send to API
4. Microphone captures audio continuously → Send as PCM chunks
5. API processes multimodal input (video + audio context)
6. API responds with audio output in real-time
7. Speaker plays AI response immediately
8. User can interrupt or ask questions anytime
9. After 2 minutes, auto-reconnect to new session

---

## Migration Strategy

### Option A: Complete Rewrite (Recommended for demo)
- Remove REST API code entirely
- Build Live API integration from scratch
- Cleaner codebase
- Better performance
- **Effort**: Medium-High

### Option B: Hybrid Approach (Safer)
- Keep REST API as fallback
- Add Live API as primary method
- Switch between them based on availability
- More complex code
- **Effort**: High

### Option C: Parallel Implementation
- Keep Phase 1 (REST) in separate branch
- Create Phase 2 (Live API) in new branch
- Test thoroughly before merging
- Can demo both approaches
- **Effort**: Medium

**Recommendation**: **Option A** for demo purposes. The app is simple enough to rewrite cleanly.

---

## Risk Assessment & Mitigation

### Risks

1. **Firebase Setup Complexity**
   - **Risk**: Firebase integration can be tricky
   - **Mitigation**: Follow official Android docs step-by-step
   - **Fallback**: Use direct WebSocket if Firebase fails

2. **API Changes (Developer Preview)**
   - **Risk**: API may change during development
   - **Mitigation**: Pin to specific model version
   - **Fallback**: Keep REST API code in git history

3. **2-Minute Session Limit**
   - **Risk**: Session expires during use
   - **Mitigation**: Auto-reconnect with context preservation
   - **Fallback**: Warn user at 1:45 mark

4. **Network Instability**
   - **Risk**: WebSocket drops on poor connection
   - **Mitigation**: Implement robust reconnection logic
   - **Fallback**: Detect and notify user, attempt reconnect

5. **1 FPS Limitation**
   - **Risk**: May miss fast-moving obstacles
   - **Mitigation**: Set user expectations, focus on static scene analysis
   - **Note**: Still better than current 5-10s single-frame delay

### Success Criteria

- ✅ Latency < 1 second from question to response
- ✅ No 503 errors (streaming bypasses rate limits)
- ✅ Continuous monitoring (not single frame)
- ✅ Natural conversation flow (bidirectional audio)
- ✅ Reliable auto-reconnect within 2-minute window
- ✅ Demo-ready user experience

---

## Comparison: REST API vs Live API

| Feature | Current (REST API) | Proposed (Live API) |
|---------|-------------------|---------------------|
| **Latency** | 5-10 seconds | <1 second |
| **Rate Limits** | 15 RPM (free tier) | Generous for streaming |
| **Session Type** | Request/Response | Persistent bidirectional |
| **Video Input** | Single frame | Continuous (1 FPS) |
| **Context** | Single frame only | Video history + audio |
| **Audio Output** | Android TTS | Native Gemini audio |
| **Interruption** | Not possible | Natural interruption |
| **Network Efficiency** | High per request | Optimized streaming |
| **Use Case** | Single question | Continuous assistance |
| **Demo Impact** | Moderate | High (impressive) |

---

## Cost Considerations

### Free Tier (Current REST API)
- 20 requests per day
- 15 requests per minute
- Frequently overloaded (503 errors)

### Live API Pricing (Unknown for free tier)
- Firebase free tier exists but Live API limits unclear
- Likely metered by session duration, not requests
- Need to test with free tier API key

**Action Required**: Test Live API with current free tier API key to confirm pricing/limits

---

## Timeline & Effort Estimate

### Phase 2A: Core Live API Implementation
- **Day 1**: Firebase setup, dependencies, basic connection
- **Day 2**: Video streaming at 1 FPS, audio integration
- **Day 3**: UI updates, session management, testing
- **Total**: 2-3 days

### Phase 2B: Enhanced Features (Optional)
- **Day 4**: Function calling, context persistence
- **Day 5**: Optimization, offline fallback
- **Total**: +1-2 days

### **Overall Estimate**: 3-5 days for complete implementation

---

## Decision Point

### Should We Implement Live API?

**YES, if:**
- ✅ Demo needs to be impressive with real-time response
- ✅ Willing to invest 3-5 days development
- ✅ Firebase setup is acceptable
- ✅ 1 FPS video processing is sufficient for use case

**NO, if:**
- ❌ Need fast implementation (< 1 day)
- ❌ Can't set up Firebase
- ❌ Need >1 FPS video analysis
- ❌ Demo deadline is immediate

### Alternative: Quick Fix (REST API with Retry Logic)

**If you need something working TODAY**:
- Add exponential backoff retry for 503 errors
- Add better error messages to user
- Optimize image compression further
- Switch to Flash-Lite model (1.5x faster)
- **Effort**: 2-3 hours
- **Result**: Band-aid solution, not transformative

---

## Recommended Next Steps

1. **Test Live API with current setup**
   - Try a simple Python script to verify API access
   - Confirm free tier limits for Live API
   - Test 1 FPS video streaming latency

2. **If Live API looks promising:**
   - Set up Firebase project
   - Begin Phase 2A implementation
   - Migrate in stages (connection → video → audio → UI)

3. **If Live API has blockers:**
   - Implement quick retry logic fix
   - Consider upgrading to paid tier
   - Explore alternative streaming approaches

---

## References

- [Gemini Live API | Android Developers](https://developer.android.com/ai/gemini/live)
- [Get started with Live API | Gemini API](https://ai.google.dev/gemini-api/docs/live)
- [Live API Capabilities Guide](https://ai.google.dev/gemini-api/docs/live-guide)
- [Firebase AI Logic - Live API](https://firebase.google.com/docs/ai-logic/live-api)
- [Gemini 2.5 Flash and Pro, Live API Launch](https://developers.googleblog.com/en/gemini-2-5-flash-pro-live-api-veo-2-gemini-api/)
- [Gemini Live API Overview (Vertex AI)](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/live-api)
- [Real-Time Multimodal Interactions with Gemini 2.0](https://developers.googleblog.com/en/gemini-2-0-level-up-your-apps-with-real-time-multimodal-interactions/)

---

## Appendix: Code Structure (Planned)

### New Classes

**LiveApiClient.java**
```
- connect()
- disconnect()
- sendVideoFrame(Bitmap frame)
- sendAudioChunk(byte[] audio)
- onResponseReceived(AudioData response)
- onError(Exception e)
```

**VideoFrameStreamer.java**
```
- startStreaming(Camera camera)
- stopStreaming()
- captureFrame() // Called every 1 second
- encodeToBase64(Bitmap frame)
- onFrameReady(String base64Frame)
```

**LiveSessionManager.java**
```
- startSession()
- stopSession()
- renewSession() // Auto-reconnect before 2-min limit
- getSessionTimeRemaining()
- onSessionExpiring()
```

### Modified Classes

**CameraPreviewActivity.java**
- Replace `GeminiClient` with `LiveApiClient`
- Replace `VoiceManager` with native audio streaming
- Add session timer UI
- Handle streaming lifecycle

**CameraPreviewFragment.java**
- Add periodic frame capture (1 FPS timer)
- Remove single-frame capture button
- Add streaming status indicator

---

**End of Plan**

*This plan provides a comprehensive roadmap for upgrading AssistEyes to use Gemini Live API for real-time video streaming and near-instant AI responses.*
