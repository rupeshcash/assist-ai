package com.usbcamera;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class CameraPreviewActivity extends AppCompatActivity {
    private static final String TAG = "CameraPreviewActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long ACTIVE_MODE_INTERVAL = 3000; // 3 seconds
    private static final String PREFS_NAME = "AssistEyesPrefs";
    private static final String KEY_CUSTOM_INSTRUCTION = "custom_instruction";

    private CameraPreviewFragment cameraFragment;
    private VoiceManager voiceManager;
    private GeminiClient geminiClient;
    private FloatingActionButton micButton;
    private SwitchMaterial activeModeSwitch;
    private MaterialButton settingsButton;
    private SharedPreferences prefs;

    private boolean isProcessing = false;
    private boolean isVoiceManagerSpeaking = false;
    private final Handler activeModeHandler = new Handler();
    private Runnable activeModeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        initializeComponents(savedInstanceState);
        if (checkPermissions()) {
            setupVoiceAndAI();
        } else {
            requestPermissions();
        }
    }

    private void initializeComponents(Bundle savedInstanceState) {
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
                    isProcessing = false;
                    updateMicButton(false);
                });
            }

            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    updateStatus("Listening...");
                    updateInstructions("Speak your question now");
                });
            }

            @Override
            public void onSpeakingStarted() {
                isVoiceManagerSpeaking = true;
            }

            @Override
            public void onSpeakingCompleted() {
                isVoiceManagerSpeaking = false;
                runOnUiThread(() -> {
                    if (!activeModeSwitch.isChecked()) {
                        isProcessing = false;
                        updateStatus("Ready");
                        updateInstructions("Tap mic to ask a question");
                        updateMicButton(false);
                    }
                });
            }
        });

        geminiClient = new GeminiClient(this, BuildConfig.GEMINI_API_KEY);

        new Handler().postDelayed(() -> {
            micButton = findViewById(R.id.mic_button);
            activeModeSwitch = findViewById(R.id.active_mode_switch);
            settingsButton = findViewById(R.id.settings_button);
            if (micButton != null) setupMicButton();
            if (activeModeSwitch != null) setupActiveModeSwitch();
            if (settingsButton != null) setupSettingsButton();
        }, 500);
    }

    private void setupMicButton() {
        micButton.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
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
    }

    private void setupActiveModeSwitch() {
        activeModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startActiveMode();
            } else {
                stopActiveMode();
            }
        });

        activeModeRunnable = () -> {
            if (activeModeSwitch.isChecked()) {
                performSilentAnalysis();
                activeModeHandler.postDelayed(activeModeRunnable, ACTIVE_MODE_INTERVAL);
            }
        };
    }

    private void setupSettingsButton() {
        settingsButton.setOnClickListener(v -> showCustomInstructionDialog());
    }

    private void showCustomInstructionDialog() {
        String currentInstruction = prefs.getString(KEY_CUSTOM_INSTRUCTION, "");

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("e.g., Alert me about obstacles at head level");
        input.setText(currentInstruction);
        input.setMinLines(3);
        input.setMaxLines(5);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("Active Mode Instruction")
                .setMessage("Customize what the AI should focus on in Active Mode:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String instruction = input.getText().toString().trim();
                    prefs.edit().putString(KEY_CUSTOM_INSTRUCTION, instruction).apply();
                    Toast.makeText(this, "Instruction saved!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (dialog, which) -> {
                    prefs.edit().remove(KEY_CUSTOM_INSTRUCTION).apply();
                    Toast.makeText(this, "Instruction reset to default", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void startActiveMode() {
        updateStatus("Active Mode ON");
        micButton.setEnabled(false);
        micButton.setAlpha(0.5f);
        activeModeHandler.post(activeModeRunnable);
    }

    private void stopActiveMode() {
        updateStatus("Active Mode OFF");
        micButton.setEnabled(true);
        micButton.setAlpha(1.0f);
        activeModeHandler.removeCallbacks(activeModeRunnable);
    }

    private void performSilentAnalysis() {
        if (isVoiceManagerSpeaking || isProcessing) return;
        Bitmap frame = captureCurrentFrame();
        if (frame == null) return;

        String customInstruction = prefs.getString(KEY_CUSTOM_INSTRUCTION, null);
        geminiClient.analyzeForObstacles(frame, customInstruction, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                if (response != null && !response.trim().isEmpty()) {
                    voiceManager.speak(response);
                    updateInstructions("AI: " + response);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Active Mode Error: " + error);
            }
        });
    }

    private void processQuery(String userQuery) {
        runOnUiThread(() -> {
            updateStatus("Processing...");
            updateInstructions("Analyzing what the camera sees...");
        });

        Bitmap frame = captureCurrentFrame();
        if (frame == null) {
            runOnUiThread(() -> {
                voiceManager.speak("Sorry, I couldn't capture the camera view. Please try again.");
                isProcessing = false;
                updateMicButton(false);
            });
            return;
        }

        geminiClient.analyzeImage(frame, userQuery, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    updateStatus("Speaking response");
                    updateInstructions("AI: " + response);
                    voiceManager.speak(response);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    String errorMsg = "Sorry, I couldn't process that.";
                    updateStatus("Error");
                    updateInstructions(errorMsg);
                    voiceManager.speak(errorMsg);
                });
            }
        });
    }

    private Bitmap captureCurrentFrame() {
        if (cameraFragment == null) return null;
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
            runOnUiThread(() -> micButton.setBackgroundTintList(ContextCompat.getColorStateList(this, isActive ? R.color.mic_button_active_color : R.color.mic_button_color)));
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceAndAI();
            } else {
                Toast.makeText(this, "Microphone permission is required.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveMode();
        if (voiceManager != null) {
            voiceManager.destroy();
        }
    }
}
