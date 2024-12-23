package com.example.autoclicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;

public class ClickOverlayService extends Service {
    private static final String TAG = "ClickOverlayService";
    private static final String CHANNEL_ID = "AutoClickerChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int CLICK_INTERVAL = 2000; // 1 second interval

    private WindowManager windowManager;
    private View floatingControlBar;
    private List<View> circles = new ArrayList<>();
    private int circleCount = 0;
    private boolean isPlaying = false;
    private Handler playHandler = new Handler();
    private int currentCircleIndex = 0;

    private boolean isReceiverRegistered = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel and start foreground service
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Initialize window manager and create the floating control bar
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingControlBar();

        // Register the receiver if not already done
        if (!isReceiverRegistered) {
            ClickReceiver.registerReceiver(this);
            isReceiverRegistered = true;
        }
    }

    // Create the notification channel for Android Oreo and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Auto Clicker Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Running Auto Clicker Service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    // Create the foreground notification
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Clicker")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_notification) // Add an icon here
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createFloatingControlBar() {
        floatingControlBar = LayoutInflater.from(this)
                .inflate(R.layout.floating_button_layout, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingControlBar, params);
        setupButtons();
        makeFloatingBarDraggable(floatingControlBar, params);
    }

    private void setupButtons() {
        ImageButton btnPlus = floatingControlBar.findViewById(R.id.btn_plus);
        ImageButton btnMinus = floatingControlBar.findViewById(R.id.btn_minus);
        ImageButton btnPlay = floatingControlBar.findViewById(R.id.btn_play);
        ImageButton btnPause = floatingControlBar.findViewById(R.id.btn_pause);
        ImageButton btnClose = floatingControlBar.findViewById(R.id.btn_save);

        btnPlus.setOnClickListener(v -> createCircle());
        btnMinus.setOnClickListener(v -> removeLastCircle());
        btnPlay.setOnClickListener(v -> startClickSequence());
        btnPause.setOnClickListener(v -> stopClickSequence());
        btnClose.setOnClickListener(v -> stopSelf());
    }

    private void createCircle() {
        circleCount++;

        FrameLayout frame = new FrameLayout(this);
        View circle = new View(this);
        circle.setBackgroundResource(R.drawable.circle_background);

        TextView numberText = new TextView(this);
        numberText.setText(String.valueOf(circleCount));
        numberText.setTextSize(16);
        numberText.setTextColor(getResources().getColor(android.R.color.white));
        numberText.setGravity(Gravity.CENTER);

        frame.addView(circle);
        frame.addView(numberText);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                75, 75,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 200;
        params.y = 200;

        windowManager.addView(frame, params);
        circles.add(frame);
        makeCircleDraggable(frame, params);
    }

    private void removeLastCircle() {
        if (!circles.isEmpty()) {
            View lastCircle = circles.remove(circles.size() - 1);
            windowManager.removeView(lastCircle);
            circleCount--;
            updateCircleNumbers();
        }
    }

    private void updateCircleNumbers() {
        for (int i = 0; i < circles.size(); i++) {
            View circle = circles.get(i);
            TextView text = (TextView) ((FrameLayout) circle).getChildAt(1);
            text.setText(String.valueOf(i + 1));
        }
    }

    private void startClickSequence() {
        if (!circles.isEmpty() && !isPlaying) {
            isPlaying = true;
            currentCircleIndex = 0;
            clickNextCircle();
        }
    }

    private void clickNextCircle() {
        if (isPlaying && currentCircleIndex < circles.size()) {
            View circle = circles.get(currentCircleIndex);
            int[] location = new int[2];
            circle.getLocationOnScreen(location);

            Intent clickIntent = new Intent();
            clickIntent.setClassName("com.example.autoclicker", "com.example.autoclicker.ClickReceiver");
            clickIntent.setAction("com.example.autoclicker.CLICK_COORDINATES");
            clickIntent.putExtra("x", location[0] + circle.getWidth() / 2f);
            clickIntent.putExtra("y", location[1] + circle.getHeight() / 2f);
            sendBroadcast(clickIntent);

            currentCircleIndex++;
            if (currentCircleIndex >= circles.size()) {
                currentCircleIndex = 0;
            }
            playHandler.postDelayed(this::clickNextCircle, CLICK_INTERVAL);
        }
    }

    private void stopClickSequence() {
        isPlaying = false;
        playHandler.removeCallbacksAndMessages(null);
    }

    private void makeFloatingBarDraggable(View view, final WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void makeCircleDraggable(View view, final WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(v, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopClickSequence();
        if (floatingControlBar != null) {
            windowManager.removeView(floatingControlBar);
        }
        for (View circle : circles) {
            windowManager.removeView(circle);
        }
        try {
            unregisterReceiver(new ClickReceiver());
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: ", e);
        }
        circles.clear();
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_NOT_STICKY;
    }
}
