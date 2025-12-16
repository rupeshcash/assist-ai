package com.usbcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiClient {
    private static final String TAG = "GeminiClient";
    private static final String MODEL_NAME = "gemini-2.5-flash";

    private GenerativeModelFutures model;
    private Executor executor;
    private Context context;

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public GeminiClient(Context context, String apiKey) {
        this.context = context;
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
                // Optimize image size for faster upload and processing
                long resizeStart = System.currentTimeMillis();
                Bitmap optimizedBitmap = optimizeImage(bitmap);
                long resizeTime = System.currentTimeMillis() - resizeStart;
                Log.d(TAG, "⚡ Image optimization took: " + resizeTime + "ms");
                Log.d(TAG, "📊 Optimized size: " + optimizedBitmap.getWidth() + "x" + optimizedBitmap.getHeight());

                // Build the prompt based on user query or use default
                String prompt = buildPrompt(userQuery);

                Log.d(TAG, "Sending request to Gemini with prompt: " + prompt);

                // Create content with image and text
                Content content = new Content.Builder()
                        .addText(prompt)
                        .addImage(optimizedBitmap)
                        .build();

                // Generate content
                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String text = result.getText();
                        Log.d(TAG, "Gemini response: " + text);
                        if (callback != null) {
                            callback.onSuccess(text);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Gemini error", t);
                        if (callback != null) {
                            callback.onError("AI processing failed: " + t.getMessage());
                        }
                    }
                }, executor);

            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini", e);
                if (callback != null) {
                    callback.onError("Failed to process image: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Optimizes image for faster API upload and processing
     * Resizes large images while maintaining aspect ratio
     * Target: max dimension of 1024px for optimal balance of speed and quality
     */
    private Bitmap optimizeImage(Bitmap original) {
        final int MAX_DIMENSION = 1024;

        int width = original.getWidth();
        int height = original.getHeight();

        // If image is already small enough, return as-is
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            Log.d(TAG, "Image already optimal size, no resize needed");
            return original;
        }

        // Calculate scale factor to maintain aspect ratio
        float scale;
        if (width > height) {
            scale = (float) MAX_DIMENSION / width;
        } else {
            scale = (float) MAX_DIMENSION / height;
        }

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Log.d(TAG, "Resizing from " + width + "x" + height + " to " + newWidth + "x" + newHeight);

        Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);

        // Free up memory from original if it's different from resized
        if (resized != original) {
            original.recycle();
        }

        return resized;
    }

    private String buildPrompt(String userQuery) {
        String systemPrompt = "You are assisting a blind person. Provide clear, concise answers in 1-2 sentences. ";

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return systemPrompt + "Describe what you see in this image, focusing on any obstacles or important objects.";
        }

        // Handle common queries
        String query = userQuery.toLowerCase().trim();

        if (query.contains("obstacle") || query.contains("ahead") || query.contains("front") || query.contains("path")) {
            return systemPrompt + "Describe any obstacles or hazards visible in this image. Mention their approximate location (left, right, center, head level, ground level).";
        } else if (query.contains("where") || query.contains("find") || query.contains("locate")) {
            return systemPrompt + "User asked: '" + userQuery + "'. Analyze the image and answer their question about location or navigation.";
        } else if (query.contains("which") || query.contains("identify") || query.contains("what is")) {
            return systemPrompt + "User asked: '" + userQuery + "'. Identify and describe the specific objects or items they're asking about.";
        } else if (query.contains("read") || query.contains("label") || query.contains("text")) {
            return systemPrompt + "Read any visible text in this image clearly and accurately.";
        } else {
            return systemPrompt + "User asked: '" + userQuery + "'. Answer their question based on what you see in the image.";
        }
    }

    // Preset prompts for common scenarios
    public static class Prompts {
        public static final String OBSTACLE_DETECTION = "Describe any obstacles or hazards visible";
        public static final String GENERAL_DESCRIPTION = "What do you see?";
        public static final String OBJECT_IDENTIFICATION = "What objects are in front of me?";
        public static final String PATH_CLEAR = "Is the path ahead clear?";
        public static final String READ_TEXT = "Read any text you see";
    }
}
