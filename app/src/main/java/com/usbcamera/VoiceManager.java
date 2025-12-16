package com.usbcamera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceManager {
    private static final String TAG = "VoiceManager";

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private VoiceCallback callback;

    public interface VoiceCallback {
        void onSpeechResult(String text);
        void onSpeechError(String error);
        void onListeningStarted();
        void onSpeakingStarted();
        void onSpeakingCompleted();
    }

    public VoiceManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        initializeTTS();
        initializeSpeechRecognizer();
    }

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    ttsReady = true;
                    Log.d(TAG, "TTS initialized successfully");
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                if (callback != null) {
                    callback.onListeningStarted();
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech started");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Audio level changed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Partial audio data received
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Speech ended");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.e(TAG, "Speech error: " + errorMessage);
                if (callback != null) {
                    callback.onSpeechError(errorMessage);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "Speech result: " + text);
                    if (callback != null) {
                        callback.onSpeechResult(text);
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Partial results available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Reserved for future events
            }
        });
    }

    public void startListening() {
        if (speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer not initialized");
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to know?");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            speechRecognizer.startListening(intent);
            Log.d(TAG, "Started listening");
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            if (callback != null) {
                callback.onSpeechError("Failed to start listening");
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void speak(String text) {
        if (!ttsReady) {
            Log.e(TAG, "TTS not ready");
            return;
        }

        if (callback != null) {
            callback.onSpeakingStarted();
        }

        textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS started");
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "TTS completed");
                if (callback != null) {
                    callback.onSpeakingCompleted();
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "TTS error");
            }
        });

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AssistEyes");
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AssistEyes");
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }
}
