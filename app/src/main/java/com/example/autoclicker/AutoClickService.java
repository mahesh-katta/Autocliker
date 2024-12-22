
// AutoClickService.java
package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
import android.util.Log;
import java.util.List;

public class AutoClickService extends AccessibilityService {
    private static final String TAG = "AutoClickService";
    private Handler handler;
    private Runnable autoClickRunnable;
    private int clickInterval = 1000;
    private boolean isAutoClicking = false;
    private static AutoClickService instance;
    private List<MainActivity.ClickPoint> clickPoints;
    private int currentClickIndex = 0;

    public static AutoClickService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used in this implementation
    }

    @Override
    public void onInterrupt() {
        stopAutoClick();
        Log.d(TAG, "Service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Service connected");
        sendStatusToActivity("AutoClicker Service Connected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAutoClick();
        instance = null;
        stopOverlayService();
        Log.d(TAG, "Service destroyed");
    }

    public void startAutoClickSequence(List<MainActivity.ClickPoint> points) {
        Log.d(TAG, "startAutoClickSequence called with " + points.size() + " points");

        if (isAutoClicking) {
            sendStatusToActivity("Auto-clicking is already running.");
            Log.d(TAG, "Auto-clicking is already running.");
            return;
        }

        this.clickPoints = points;
        currentClickIndex = 0;
        startOverlayService();

        handler = new Handler(Looper.getMainLooper());
        autoClickRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentClickIndex >= clickPoints.size()) {
                    currentClickIndex = 0;
                }

                MainActivity.ClickPoint point = clickPoints.get(currentClickIndex);
                Log.d(TAG, "Clicking point " + point.getNumber() + " at (" + point.getX() + ", " + point.getY() + ")");
                performClick(point.getX(), point.getY());
                currentClickIndex++;

                handler.postDelayed(this, clickInterval);
            }
        };
        handler.post(autoClickRunnable);
        isAutoClicking = true;

        sendStatusToActivity("Auto-clicking sequence started");
        Log.d(TAG, "Started auto-clicking sequence");
    }

    public void stopAutoClick() {
        Log.d(TAG, "stopAutoClick called");

        if (!isAutoClicking) {
            sendStatusToActivity("Auto-clicking is already stopped.");
            Log.d(TAG, "Auto-clicking is already stopped.");
            return;
        }

        if (handler != null) {
            handler.removeCallbacks(autoClickRunnable);
            Log.d(TAG, "Removed callbacks from handler");
        }
        isAutoClicking = false;
        stopOverlayService();

        sendStatusToActivity("Auto-clicking stopped.");
        Log.d(TAG, "Stopped auto-clicking process");
    }

    private void startOverlayService() {
        Intent overlayIntent = new Intent(this, ClickOverlayService.class);
        startService(overlayIntent);
    }

    private void stopOverlayService() {
        stopService(new Intent(this, ClickOverlayService.class));
    }

    private void performClick(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Path path = new Path();
                path.moveTo(x, y);

                GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 100);
                GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();

                Log.d(TAG, "Dispatching gesture at coordinates: (" + x + ", " + y + ")");

                boolean dispatched = dispatchGesture(gesture, null, null);
                if (dispatched) {
                    Log.d(TAG, "Gesture dispatched successfully");
                } else {
                    Log.e(TAG, "Failed to dispatch gesture");
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException in performClick: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Gesture not dispatched: SDK version too low");
        }
    }

    private void sendStatusToActivity(String message) {
        Intent intent = new Intent("com.example.autoclicker.STATUS_UPDATE");
        intent.putExtra("status", message);
        sendBroadcast(intent);
        Log.d(TAG, "Status sent to activity: " + message);
    }
}
