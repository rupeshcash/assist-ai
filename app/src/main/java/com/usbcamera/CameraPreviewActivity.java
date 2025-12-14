package com.usbcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class CameraPreviewActivity extends AppCompatActivity {

    private UsbManager usbManager;
    private TextureView cameraView;
    private TextView statusText;
    private TextView instructionsText;
    private Button backButton;
    private UsbDevice usbCamera;
    private UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        cameraView = findViewById(R.id.camera_texture_view);
        statusText = findViewById(R.id.preview_status_text);
        instructionsText = findViewById(R.id.instructions_text);
        backButton = findViewById(R.id.back_button);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        backButton.setOnClickListener(v -> finish());

        // Register USB receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }

        detectAndConnectCamera();
    }

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

    private void detectAndConnectCamera() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            statusText.setText("No USB devices found");
            showInstructions("Please connect a USB camera via OTG adapter");
            return;
        }

        for (UsbDevice device : deviceList.values()) {
            if (isVideoDevice(device)) {
                usbCamera = device;
                connectToCamera(device);
                return;
            }
        }

        // Try first device anyway
        if (!deviceList.isEmpty()) {
            UsbDevice device = deviceList.values().iterator().next();
            usbCamera = device;
            connectToCamera(device);
        } else {
            showInstructions("No USB camera detected");
        }
    }

    private boolean isVideoDevice(UsbDevice device) {
        int deviceClass = device.getDeviceClass();
        return deviceClass == 14 || deviceClass == 239 || deviceClass == 0;
    }

    private void connectToCamera(UsbDevice device) {
        if (!usbManager.hasPermission(device)) {
            statusText.setText("No USB permission - please grant permission from main screen first");
            showInstructions("Go back and tap 'Scan for USB Devices' to grant permission");
            return;
        }

        try {
            connection = usbManager.openDevice(device);

            if (connection != null) {
                statusText.setText("✓ Connected to USB Camera!");

                // Get camera info
                StringBuilder info = new StringBuilder();
                info.append("Camera Connected Successfully!\n\n");
                info.append("Device: ").append(device.getDeviceName()).append("\n");
                info.append("Vendor ID: ").append(device.getVendorId()).append("\n");
                info.append("Product ID: ").append(device.getProductId()).append("\n");
                info.append("Interfaces: ").append(device.getInterfaceCount()).append("\n\n");

                // Find video streaming interface
                boolean foundVideoInterface = false;
                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    UsbInterface usbInterface = device.getInterface(i);
                    info.append("Interface ").append(i).append(":\n");
                    info.append("  Class: ").append(usbInterface.getInterfaceClass()).append("\n");
                    info.append("  Subclass: ").append(usbInterface.getInterfaceSubclass()).append("\n");
                    info.append("  Endpoints: ").append(usbInterface.getEndpointCount()).append("\n");

                    // USB Video Class = 14
                    if (usbInterface.getInterfaceClass() == 14) {
                        foundVideoInterface = true;
                        info.append("  ✓ Video Interface Found!\n");
                    }
                }

                if (foundVideoInterface) {
                    info.append("\n✓ This is a UVC camera!\n\n");
                    info.append("To show live video, you need:\n");
                    info.append("• UVC protocol implementation\n");
                    info.append("• MJPEG/H264 decoder\n");
                    info.append("• Frame rendering pipeline\n\n");
                    info.append("The camera is ready and can be accessed programmatically.");
                } else {
                    info.append("\nDevice may not be a standard UVC camera.");
                }

                showInstructions(info.toString());

                Toast.makeText(this,
                    "Camera detected and connected!\nFile descriptor: " + connection.getFileDescriptor(),
                    Toast.LENGTH_LONG).show();

            } else {
                statusText.setText("Failed to open camera");
                showInstructions("Could not open USB device connection");
            }
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
            showInstructions("Connection failed: " + e.getMessage());
        }
    }

    private void showInstructions(String text) {
        instructionsText.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }

        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
