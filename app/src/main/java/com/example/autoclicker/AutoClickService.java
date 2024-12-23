// AutoClickService.java
package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AutoClickService extends AccessibilityService {
    private static final String TAG = "AutoClickService";
    private static AutoClickService instance;

    public static AutoClickService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Keep minimal to avoid interference
        if (event == null) return;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        // Configure service capabilities
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        setServiceInfo(info);

        Log.d(TAG, "Service connected and configured");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Service destroyed");
    }

    public void performClick(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, 1);

            gestureBuilder.addStroke(clickStroke);

            boolean result = dispatchGesture(
                    gestureBuilder.build(),
                    new GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            Log.d(TAG, "Gesture completed successfully");
                            super.onCompleted(gestureDescription);
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            Log.e(TAG, "Gesture cancelled");
                            super.onCancelled(gestureDescription);
                        }
                    },
                    null
            );

            Log.d(TAG, "Dispatching click at: (" + x + ", " + y + ") Result: " + result);
        }
    }
}