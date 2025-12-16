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
                            if (t instanceof ServerException && t.getMessage().contains("503")) {
                                callback.onError("The AI model is currently overloaded. Please try again in a moment.");
                            } else {
                                callback.onError("AI processing failed: " + t.getMessage());
                            }
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

    private Bitmap optimizeImage(Bitmap original) {
        final int MAX_DIMENSION = 1024;
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
        String systemPrompt = "You are assisting a blind person. Provide clear, concise answers in 1-2 sentences. ";
        String query = (userQuery != null) ? userQuery.toLowerCase().trim() : "";

        if (query.isEmpty() || query.contains("describe")) {
            return systemPrompt + "Describe what you see, focusing on obstacles or important objects.";
        } else if (query.contains("obstacle") || query.contains("ahead") || query.contains("path")) {
            return systemPrompt + "Describe any obstacles or hazards visible. Mention their approximate location.";
        } else if (query.contains("read") || query.contains("text")) {
            return systemPrompt + "Read any visible text in this image clearly.";
        } else {
            return systemPrompt + "User asked: '" + userQuery + "'. Answer their question based on the image.";
        }
    }
}
