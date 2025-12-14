package com.usbcamera;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CameraPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CameraPreviewFragment())
                .commit();
        }
    }
}
