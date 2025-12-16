package com.usbcamera;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.base.CameraFragment;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.widget.AspectRatioTextureView;
import com.jiangdg.ausbc.widget.IAspectRatio;

public class CameraPreviewFragment extends CameraFragment {
    private static final String TAG = "CameraPreviewFragment";

    private AspectRatioTextureView cameraView;
    private TextView statusText;
    private TextView instructionsText;
    private Button backButton;
    private View rootView;

    @Nullable
    @Override
    public View getRootView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        rootView = inflater.inflate(R.layout.fragment_camera_preview, container, false);

        cameraView = rootView.findViewById(R.id.camera_texture_view);
        statusText = rootView.findViewById(R.id.preview_status_text);
        instructionsText = rootView.findViewById(R.id.instructions_text);
        backButton = rootView.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        statusText.setText("Initializing...");
        instructionsText.setText("Waiting for USB camera...");

        return rootView;
    }

    @Nullable
    @Override
    public IAspectRatio getCameraView() {
        return cameraView;
    }

    @Nullable
    @Override
    public ViewGroup getCameraViewContainer() {
        return rootView != null ? (ViewGroup) rootView.findViewById(R.id.camera_container) : null;
    }

    @NonNull
    @Override
    public CameraRequest getCameraRequest() {
        return new CameraRequest.Builder()
                .setPreviewWidth(640)
                .setPreviewHeight(480)
                .create();
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public void onCameraState(@NonNull MultiCameraClient.ICamera self, @NonNull ICameraStateCallBack.State code, @Nullable String msg) {
        Log.d(TAG, "Camera state: " + code + ", msg: " + msg);

        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            switch (code) {
                case OPENED:
                    statusText.setText("Live Preview Active");
                    instructionsText.setText("Camera is streaming\nResolution: 640x480");
                    break;
                case CLOSED:
                    statusText.setText("Camera Closed");
                    instructionsText.setText("Camera has been closed");
                    break;
                case ERROR:
                    statusText.setText("Error");
                    instructionsText.setText("Camera error: " + (msg != null ? msg : "Unknown"));
                    break;
            }
        });
    }

    /**
     * Captures the current camera frame as a Bitmap
     * @return Bitmap of current frame, or null if not available
     */
    public Bitmap captureFrame() {
        if (cameraView == null) {
            Log.e(TAG, "Camera view is null");
            return null;
        }

        try {
            Bitmap bitmap = cameraView.getBitmap();
            if (bitmap != null) {
                Log.d(TAG, "Frame captured: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } else {
                Log.w(TAG, "Captured bitmap is null");
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturing frame", e);
            return null;
        }
    }

    /**
     * Updates the status text (called from Activity)
     */
    public void updateStatus(String status) {
        if (statusText != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> statusText.setText(status));
        }
    }

    /**
     * Updates the instructions text (called from Activity)
     */
    public void updateInstructions(String instructions) {
        if (instructionsText != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> instructionsText.setText(instructions));
        }
    }
}
