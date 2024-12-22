package com.example.autoclicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
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
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ClickOverlayService extends Service {
    private static final String TAG = "ClickOverlayService";
    private WindowManager windowManager;
    private View floatingControlBar;
    private List<View> circles = new ArrayList<>();
    private int circleCount = 0;
    private boolean isPlaying = false;
    private Handler playHandler = new Handler();
    private int currentCircleIndex = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingControlBar();
    }

    private void createFloatingControlBar() {
        floatingControlBar = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);

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
        setupButtons(params);
        makeFloatingBarDraggable(floatingControlBar, params);
    }

    private void setupButtons(final WindowManager.LayoutParams params) {
        ImageButton btnPlus = floatingControlBar.findViewById(R.id.btn_plus);
        ImageButton btnMinus = floatingControlBar.findViewById(R.id.btn_minus);
        ImageButton btnPlay = floatingControlBar.findViewById(R.id.btn_play);
        ImageButton btnPause = floatingControlBar.findViewById(R.id.btn_pause);

        btnPlus.setOnClickListener(v -> createCircle());
        btnMinus.setOnClickListener(v -> removeLastCircle());
        btnPlay.setOnClickListener(v -> startClickSequence());
        btnPause.setOnClickListener(v -> stopClickSequence());
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

            Intent clickIntent = new Intent(this, AutoClickService.class);
            clickIntent.putExtra("x", location[0] + circle.getWidth() / 2);
            clickIntent.putExtra("y", location[1] + circle.getHeight() / 2);
            startService(clickIntent);

            currentCircleIndex++;
            if (currentCircleIndex < circles.size()) {
                playHandler.postDelayed(this::clickNextCircle, 1000);
            } else {
                currentCircleIndex = 0;
                playHandler.postDelayed(this::clickNextCircle, 1000);
            }
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
        if (floatingControlBar != null) {
            windowManager.removeView(floatingControlBar);
        }
        for (View circle : circles) {
            windowManager.removeView(circle);
        }
        stopClickSequence();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }
}