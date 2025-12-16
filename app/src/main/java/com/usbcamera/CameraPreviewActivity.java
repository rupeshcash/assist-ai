package com.usbcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CameraPreviewActivity extends AppCompatActivity {
    private static final String TAG = "CameraPreviewActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String GEMINI_API_KEY = "YOUR_API_KEY_HERE"; // TODO: Replace with your actual API key

    private CameraPreviewFragment cameraFragment;
    private VoiceManager voiceManager;
    private GeminiClient geminiClient;
    private FloatingActionButton micButton;

    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        // Initialize components
        initializeComponents(savedInstanceState);

        // Check and request permissions
        if (checkPermissions()) {
            setupVoiceAndAI();
        } else {
            requestPermissions();
        }
    }

    private void initializeComponents(Bundle savedInstanceState) {
        // Get camera fragment
        if (savedInstanceState == null) {
            cameraFragment = new CameraPreviewFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, cameraFragment)
                    .commit();
        } else {
            cameraFragment = (CameraPreviewFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
        }
    }

    private void setupVoiceAndAI() {
        // Initialize VoiceManager
        voiceManager = new VoiceManager(this, new VoiceManager.VoiceCallback() {
            @Override
            public void onSpeechResult(String text) {
                Log.d(TAG, "User said: " + text);
                processQuery(text);
            }

            @Override
            public void onSpeechError(String error) {
                Log.e(TAG, "Speech error: " + error);
                runOnUiThread(() -> {
                    updateStatus("Listening failed");
                    Toast.makeText(CameraPreviewActivity.this, "Couldn't hear you. Try again.", Toast.LENGTH_SHORT).show();
                    isProcessing = false;
                    updateMicButton(false);
                });
            }

            @Override
            public void onListeningStarted() {
                Log.d(TAG, "Listening started");
                runOnUiThread(() -> {
                    updateStatus("Listening...");
                    updateInstructions("Speak your question now");
                });
            }

            @Override
            public void onSpeakingStarted() {
                Log.d(TAG, "Speaking started");
            }

            @Override
            public void onSpeakingCompleted() {
                Log.d(TAG, "Speaking completed");
                runOnUiThread(() -> {
                    isProcessing = false;
                    updateStatus("Ready");
                    updateInstructions("Tap mic to ask a question");
                    updateMicButton(false);
                });
            }
        });

        // Initialize GeminiClient
        geminiClient = new GeminiClient(this, GEMINI_API_KEY);

        // Setup mic button - delay to ensure fragment is ready
        micButton = findViewById(R.id.mic_button);
        if (micButton == null) {
            // Fragment not yet inflated, retry after a delay
            new android.os.Handler().postDelayed(() -> {
                micButton = findViewById(R.id.mic_button);
                if (micButton != null) {
                    setupMicButton();
                }
            }, 500);
        } else {
            setupMicButton();
        }
    }

    private void setupMicButton() {
        if (micButton == null) return;

        micButton.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(this, "Please wait, processing...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!checkPermissions()) {
                requestPermissions();
                return;
            }

            isProcessing = true;
            updateMicButton(true);
            voiceManager.startListening();
        });

        Log.d(TAG, "Mic button setup complete");
    }

    private void processQuery(String userQuery) {
        runOnUiThread(() -> {
            updateStatus("Processing...");
            updateInstructions("Analyzing what the camera sees...");
        });

        // Capture frame from camera
        Bitmap frame = captureCurrentFrame();
        if (frame == null) {
            runOnUiThread(() -> {
                voiceManager.speak("Sorry, I couldn't capture the camera view. Please try again.");
                isProcessing = false;
                updateMicButton(false);
            });
            return;
        }

        // Send to Gemini
        geminiClient.analyzeImage(frame, userQuery, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "AI response: " + response);
                runOnUiThread(() -> {
                    updateStatus("Speaking response");
                    updateInstructions("AI: " + response);
                    voiceManager.speak(response);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "AI error: " + error);
                runOnUiThread(() -> {
                    String errorMsg = "Sorry, I couldn't process that. Please check your internet connection and try again.";
                    updateStatus("Error");
                    updateInstructions(errorMsg);
                    voiceManager.speak(errorMsg);
                });
            }
        });
    }

    private Bitmap captureCurrentFrame() {
        if (cameraFragment == null) {
            Log.e(TAG, "Camera fragment is null");
            return null;
        }
        return cameraFragment.captureFrame();
    }

    private void updateStatus(String status) {
        if (cameraFragment != null) {
            cameraFragment.updateStatus(status);
        }
    }

    private void updateInstructions(String instructions) {
        if (cameraFragment != null) {
            cameraFragment.updateInstructions(instructions);
        }
    }

    private void updateMicButton(boolean isActive) {
        if (micButton != null) {
            runOnUiThread(() -> {
                if (isActive) {
                    micButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336)); // Red
                } else {
                    micButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
                }
            });
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceAndAI();
                Toast.makeText(this, "Permission granted! Tap the mic to start.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission is required to use voice features", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.destroy();
        }
    }
}
