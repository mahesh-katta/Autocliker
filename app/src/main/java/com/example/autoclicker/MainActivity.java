// MainActivity.java
package com.example.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private Button startStopButton, addCircleButton, removeCircleButton;
    private LinearLayout circleContainer;
    private boolean isAutoClicking = false;
    private List<ClickPoint> clickPoints;
    private int circleCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        clickPoints = new ArrayList<>();
        initializeViews();
        setupListeners();

        IntentFilter filter = new IntentFilter("com.example.autoclicker.STATUS_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    private void initializeViews() {
        startStopButton = findViewById(R.id.startStopButton);
        addCircleButton = findViewById(R.id.addCircleButton);
        removeCircleButton = findViewById(R.id.removeCircleButton);
        circleContainer = findViewById(R.id.circleContainer);
    }

    private void setupListeners() {
        startStopButton.setOnClickListener(v -> {
            Log.d(TAG, "Start/Stop Button clicked");
            checkAndRequestPermissions();
        });

        addCircleButton.setOnClickListener(v -> addNewClickPoint());

        removeCircleButton.setOnClickListener(v -> removeLastClickPoint());
    }

    private void addNewClickPoint() {
        circleCounter++;
        ClickPoint clickPoint = new ClickPoint(circleCounter);
        clickPoints.add(clickPoint);
        updateCircleDisplay();
    }

    private void removeLastClickPoint() {
        if (!clickPoints.isEmpty()) {
            clickPoints.remove(clickPoints.size() - 1);
            circleCounter--;
            updateCircleDisplay();
        }
    }

    private void updateCircleDisplay() {
        circleContainer.removeAllViews();
        for (ClickPoint point : clickPoints) {
            // Add visual representation of circles here if needed
            // This is where you'd add UI elements to show the circles
        }
    }

    private void checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Please enable overlay permission", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isAccessibilityServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please enable the AutoClick Accessibility Service", Toast.LENGTH_LONG).show();
            return;
        }

        toggleAutoClick();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleAutoClick() {
        Log.d(TAG, "Toggling auto-click");

        if (clickPoints.isEmpty()) {
            Toast.makeText(this, "Please add at least one click point", Toast.LENGTH_SHORT).show();
            return;
        }

        AutoClickService service = AutoClickService.getInstance();
        if (service != null) {
            if (!isAutoClicking) {
                service.startAutoClickSequence(clickPoints);
                isAutoClicking = true;
                startStopButton.setText("Stop Auto-Click");
            } else {
                service.stopAutoClick();
                isAutoClicking = false;
                startStopButton.setText("Start Auto-Click");
            }
        } else {
            Toast.makeText(this, "Service not connected. Please enable accessibility service.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String expectedServiceName = getPackageName() + "/" + AutoClickService.class.getName();
        return enabledServices != null && enabledServices.contains(expectedServiceName);
    }

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(statusReceiver);
            if (isAutoClicking) {
                AutoClickService service = AutoClickService.getInstance();
                if (service != null) {
                    service.stopAutoClick();
                }
                stopService(new Intent(this, ClickOverlayService.class));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: ", e);
        }
    }

    public static class ClickPoint {
        private final int number;
        private float x;
        private float y;

        public ClickPoint(int number) {
            this.number = number;
            // Set default coordinates or random coordinates as needed
            this.x = 500; // Default or calculate based on screen size
            this.y = 500; // Default or calculate based on screen size
        }

        public int getNumber() {
            return number;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public void setCoordinates(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
