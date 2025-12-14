package com.usbcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

public class CameraPreviewActivity extends AppCompatActivity {
    private static final String TAG = "CameraPreview";
    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;

    private TextureView cameraView;
    private TextView statusText;
    private TextView instructionsText;
    private Button backButton;

    private USBMonitor usbMonitor;
    private UVCCamera uvcCamera;
    private Surface previewSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        cameraView = findViewById(R.id.camera_texture_view);
        statusText = findViewById(R.id.preview_status_text);
        instructionsText = findViewById(R.id.instructions_text);
        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());

        // Initialize USB monitor
        usbMonitor = new USBMonitor(this, onDeviceConnectListener);

        // Set up TextureView listener
        cameraView.setSurfaceTextureListener(surfaceTextureListener);

        // Register USB detach receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }

        statusText.setText("Initializing camera...");
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface available: " + width + "x" + height);
            previewSurface = new Surface(surface);
            startCameraIfReady();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface size changed: " + width + "x" + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "Surface destroyed");
            stopCamera();
            if (previewSurface != null) {
                previewSurface.release();
                previewSurface = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Frame updates - can be used for statistics
        }
    };

    private final USBMonitor.OnDeviceConnectListener onDeviceConnectListener =
            new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Log.d(TAG, "USB Device attached: " + device.getDeviceName());
            statusText.setText("Camera attached!");
            usbMonitor.requestPermission(device);
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Log.d(TAG, "USB Device connected: " + device.getDeviceName());
            statusText.setText("Connected to camera!");

            try {
                uvcCamera = new UVCCamera();
                uvcCamera.open(ctrlBlock);

                Log.d(TAG, "Camera opened successfully");
                instructionsText.setText(
                    "Camera Information:\n" +
                    "Device: " + device.getDeviceName() + "\n" +
                    "Vendor ID: " + device.getVendorId() + "\n" +
                    "Product ID: " + device.getProductId() + "\n\n" +
                    "Status: Connected and ready\n" +
                    "Preview: Starting..."
                );

                startCameraIfReady();

            } catch (Exception e) {
                Log.e(TAG, "Error opening camera", e);
                statusText.setText("Error: " + e.getMessage());
                instructionsText.setText("Failed to open camera:\n" + e.getMessage());
                releaseCamera();
            }
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Log.d(TAG, "USB Device disconnected: " + device.getDeviceName());
            statusText.setText("Camera disconnected");
            Toast.makeText(CameraPreviewActivity.this,
                "USB camera disconnected", Toast.LENGTH_SHORT).show();
            stopCamera();
        }

        @Override
        public void onDettach(UsbDevice device) {
            Log.d(TAG, "USB Device detached: " + device.getDeviceName());
            statusText.setText("Camera detached");
            stopCamera();
        }

        @Override
        public void onCancel(UsbDevice device) {
            Log.d(TAG, "USB permission cancelled");
            statusText.setText("Permission denied");
            instructionsText.setText("Camera permission was denied.\nPlease grant permission to use the camera.");
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                statusText.setText("Camera disconnected");
                Toast.makeText(CameraPreviewActivity.this,
                    "USB camera was disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void startCameraIfReady() {
        if (uvcCamera != null && previewSurface != null) {
            try {
                Log.d(TAG, "Starting camera preview...");

                // Set preview size and format
                uvcCamera.setPreviewSize(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    UVCCamera.FRAME_FORMAT_MJPEG
                );

                uvcCamera.setPreviewDisplay(previewSurface);
                uvcCamera.startPreview();

                statusText.setText("✓ Live Preview Active");
                instructionsText.setText(
                    instructionsText.getText() + "\n\n" +
                    "Preview: ACTIVE (" + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT + ")\n" +
                    "Format: MJPEG"
                );

                Log.d(TAG, "Preview started successfully");

            } catch (Exception e) {
                Log.e(TAG, "Error starting preview", e);
                statusText.setText("Error starting preview");
                instructionsText.setText("Failed to start preview:\n" + e.getMessage());
            }
        } else {
            Log.d(TAG, "Not ready to start preview - camera: " + uvcCamera + ", surface: " + previewSurface);
        }
    }

    private void stopCamera() {
        if (uvcCamera != null) {
            try {
                uvcCamera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping preview", e);
            }
            releaseCamera();
        }
    }

    private void releaseCamera() {
        if (uvcCamera != null) {
            try {
                uvcCamera.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying camera", e);
            }
            uvcCamera = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (usbMonitor != null) {
            usbMonitor.register();
        }
    }

    @Override
    protected void onStop() {
        if (usbMonitor != null) {
            usbMonitor.unregister();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }

        stopCamera();

        if (usbMonitor != null) {
            usbMonitor.destroy();
            usbMonitor = null;
        }
    }
}
