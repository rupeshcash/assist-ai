package com.usbcamera;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.usbcamera.USB_PERMISSION";

    private UsbManager usbManager;
    private TextView statusText;
    private TextView deviceInfoText;
    private Button scanButton;
    private Button previewButton;
    private UsbDevice usbCamera;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Toast.makeText(context, "Received broadcast: " + action, Toast.LENGTH_SHORT).show();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                statusText.setText("USB Device Attached!");
                checkForUsbCamera();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                statusText.setText("USB Device Detached");
                deviceInfoText.setText("");
                usbCamera = null;
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                    Toast.makeText(MainActivity.this,
                        "Permission result: " + (granted ? "GRANTED" : "DENIED"),
                        Toast.LENGTH_LONG).show();

                    if (granted) {
                        if (device != null) {
                            statusText.setText("✓ Permission Granted!");
                            connectToCamera(device);
                        } else {
                            statusText.setText("Permission granted but device is null");
                        }
                    } else {
                        statusText.setText("✗ Permission Denied by user");
                        Toast.makeText(MainActivity.this,
                            "You must grant USB permission to use the camera",
                            Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        deviceInfoText = findViewById(R.id.device_info_text);
        scanButton = findViewById(R.id.scan_button);
        previewButton = findViewById(R.id.preview_button);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        scanButton.setOnClickListener(v -> checkForUsbCamera());

        previewButton.setOnClickListener(v -> {
            if (usbCamera != null && usbManager.hasPermission(usbCamera)) {
                Intent intent = new Intent(MainActivity.this, CameraPreviewActivity.class);
                intent.putExtra("usb_device", usbCamera);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No camera connected or permission not granted", Toast.LENGTH_SHORT).show();
            }
        });

        // Register USB receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);

        // Use ContextCompat for automatic flag handling across all Android versions
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Check for already connected devices
        checkForUsbCamera();
    }

    private void checkForUsbCamera() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            statusText.setText("No USB devices found. Please connect a USB camera via OTG.");
            deviceInfoText.setText("");
            return;
        }

        statusText.setText("Found " + deviceList.size() + " USB device(s)");

        for (UsbDevice device : deviceList.values()) {
            // Check if it's a video device (class 14 = Video)
            if (isVideoDevice(device)) {
                usbCamera = device;
                displayDeviceInfo(device);
                requestPermission(device);
                return;
            }
        }

        // If no video device found, show first device anyway
        if (!deviceList.isEmpty()) {
            UsbDevice device = deviceList.values().iterator().next();
            displayDeviceInfo(device);
            statusText.setText("USB device found (may not be a camera)");
            requestPermission(device);
        }
    }

    private boolean isVideoDevice(UsbDevice device) {
        // USB Video Class = 14
        // Some cameras report as class 239 (Miscellaneous)
        int deviceClass = device.getDeviceClass();
        return deviceClass == 14 || deviceClass == 239 || deviceClass == 0;
    }

    private void displayDeviceInfo(UsbDevice device) {
        StringBuilder info = new StringBuilder();
        info.append("Device Name: ").append(device.getDeviceName()).append("\n");
        info.append("Vendor ID: ").append(device.getVendorId()).append("\n");
        info.append("Product ID: ").append(device.getProductId()).append("\n");
        info.append("Device Class: ").append(device.getDeviceClass()).append("\n");
        info.append("Device Subclass: ").append(device.getDeviceSubclass()).append("\n");
        info.append("Interface Count: ").append(device.getInterfaceCount()).append("\n");

        deviceInfoText.setText(info.toString());
    }

    private void requestPermission(UsbDevice device) {
        if (usbManager.hasPermission(device)) {
            statusText.setText("Already have permission");
            connectToCamera(device);
        } else {
            statusText.setText("Requesting USB permission...\nPlease allow access in the dialog");

            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(getPackageName());

            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );

            usbManager.requestPermission(device, permissionIntent);

            Toast.makeText(this,
                "Permission dialog should appear - please allow USB access",
                Toast.LENGTH_LONG).show();
        }
    }

    private void connectToCamera(UsbDevice device) {
        try {
            UsbDeviceConnection connection = usbManager.openDevice(device);

            if (connection != null) {
                statusText.setText("✓ Connected to USB Camera!");

                Toast.makeText(this,
                    "USB Camera connected successfully!\n" +
                    "File Descriptor: " + connection.getFileDescriptor(),
                    Toast.LENGTH_LONG).show();

                // Camera is now ready to use
                // In a full implementation, you would:
                // 1. Configure the camera endpoints
                // 2. Set up video streaming
                // 3. Display preview

            } else {
                statusText.setText("Failed to open device");
                Toast.makeText(this, "Could not open USB device", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
            Toast.makeText(this, "Error connecting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }
    }
}
