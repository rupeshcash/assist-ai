package com.usbcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.ServerException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiClient {
    private static final String TAG = "GeminiClient";
    private static final String MODEL_NAME = "gemini-2.5-flash-lite";

    private final GenerativeModelFutures model;
    private final Executor executor;

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public GeminiClient(Context context, String apiKey) {
        this.executor = Executors.newSingleThreadExecutor();
        GenerativeModel gm = new GenerativeModel(MODEL_NAME, apiKey);
        this.model = GenerativeModelFutures.from(gm);
        Log.d(TAG, "Gemini client initialized");
    }

    public void analyzeImage(Bitmap bitmap, String userQuery, GeminiCallback callback) {
        if (bitmap == null) {
            callback.onError("No image provided");
            return;
        }

        executor.execute(() -> {
            try {
                Bitmap optimizedBitmap = optimizeImage(bitmap);
                String prompt = buildPrompt(userQuery);

                Content content = new Content.Builder()
                        .addText(prompt)
                        .addImage(optimizedBitmap)
                        .build();

                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        callback.onSuccess(result.getText());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleFailure(t, callback);
                    }
                }, executor);
            } catch (Exception e) {
                callback.onError("Failed to process image: " + e.getMessage());
            }
        });
    }

    public void analyzeForObstacles(Bitmap bitmap, String customInstruction, GeminiCallback callback) {
        if (bitmap == null) {
            callback.onError("No image provided");
            return;
        }

        executor.execute(() -> {
            try {
                Bitmap optimizedBitmap = optimizeImage(bitmap);
                String prompt = buildObstaclePrompt(customInstruction);

                Content content = new Content.Builder()
                        .addText(prompt)
                        .addImage(optimizedBitmap)
                        .build();

                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String text = result.getText();
                        if (text != null && text.trim().equalsIgnoreCase("clear")) {
                            callback.onSuccess(""); // Send empty string if clear
                        } else {
                            callback.onSuccess(text);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleFailure(t, callback);
                    }
                }, executor);
            } catch (Exception e) {
                callback.onError("Failed to process image: " + e.getMessage());
            }
        });
    }

    private void handleFailure(Throwable t, GeminiCallback callback) {
        Log.e(TAG, "Gemini error", t);
        if (t instanceof ServerException && t.getMessage().contains("503")) {
            callback.onError("The AI model is currently overloaded.");
        } else {
            callback.onError("AI processing failed: " + t.getMessage());
        }
    }

    private Bitmap optimizeImage(Bitmap original) {
        final int MAX_DIMENSION = 512; // Reduced from 768 for faster upload
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return original;
        }

        float scale = (width > height) ? (float) MAX_DIMENSION / width : (float) MAX_DIMENSION / height;
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        if (resized != original) {
            original.recycle();
        }
        return resized;
    }

    private String buildPrompt(String userQuery) {
        String systemPrompt = "You are assisting a person with visual impairments. Provide clear, concise answers in 1-2 sentences max. strictly. be concise as possible. You must not mention image or something. Simulate yourself as you are person's smart cane with eyes. Help them being a personal assistant";
        String query = (userQuery != null) ? userQuery.toLowerCase().trim() : "";
        String finalPrompt = "";
        if(!query.isEmpty()) {
            finalPrompt = systemPrompt + "User asked: '" + userQuery + "'. Answer their question based on the image.";
        }
        System.out.println("Prompt:" +finalPrompt );
        return finalPrompt;
    }

    private String buildObstaclePrompt(String customInstruction) {
        if (customInstruction != null && !customInstruction.trim().isEmpty()) {
            return "You are assisting a blind person. " + customInstruction + " If the path is clear, respond with only the word 'clear'.";
        } else {
            return "You are assisting a blind person. Describe obstacles or important objects. If the path is clear, respond with only the word 'clear'.";
        }
    }
}
