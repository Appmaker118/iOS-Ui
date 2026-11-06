package com.replica.iosui.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.replica.iosui.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IOSLockScreenService extends Service {

    private WindowManager windowManager;
    private View lockView;
    private View lockRoot;
    private TextView clockText;
    private TextView dateText;
    private BroadcastReceiver screenReceiver;
    
    private WindowManager.LayoutParams lockParams;
    private float initialTouchY = 0f;
    private int displayHeight = 0;
    private boolean isLockVisible = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        // Inflate the layout
        lockView = LayoutInflater.from(this).inflate(R.layout.overlay_lockscreen, null);
        lockRoot = lockView.findViewById(R.id.lockRoot);
        clockText = lockView.findViewById(R.id.lockClockText);
        dateText = lockView.findViewById(R.id.lockDateText);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE;

        lockParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        lockParams.gravity = Gravity.TOP | Gravity.START;

        windowManager.addView(lockView, lockParams);

        // Configure Time Updates and Touch Engine Actions
        updateClockData();
        setupUnlockGesture();
        setupScreenStateListeners();
    }

    private void updateClockData() {
        SimpleDateFormat clockFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.toUpperCase());
        
        clockText.setText(clockFormat.format(new Date()));
        dateText.setText(dateFormat.format(new Date()));
    }

    private void setupScreenStateListeners() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    updateClockData();
                    showLockScreen();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
    }

    private void showLockScreen() {
        if (!isLockVisible) {
            isLockVisible = true;
            lockRoot.setVisibility(View.VISIBLE);
            lockRoot.setAlpha(0f);
            
            // Fade in gently matching the classic iOS layout wake physics animation loop
            new com.replica.iosui.physics.IOSSpringAnimator(lockRoot).animateTo(0f, 1f, val -> lockRoot.setAlpha(val));
        }
    }

    private void setupUnlockGesture() {
        // Swipe Up to Unlock Gesture Engine Pipeline
        lockRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float rawY = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchY = rawY;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaY = rawY - initialTouchY;
                        if (deltaY < 0) { // Moving upwards onto screen space boundaries
                            lockRoot.setTranslationY(deltaY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float currentTransY = lockRoot.getTranslationY();
                        // If pulled upward beyond 30% of screen size metrics, dismiss lock
                        if (currentTransY < -displayHeight * 0.30f) {
                            new com.replica.iosui.physics.IOSSpringAnimator(lockRoot).animateTo(currentTransY, -displayHeight, val -> {
                                lockRoot.setTranslationY(val);
                                if (val <= -displayHeight + 10f) {
                                    dismissLockScreen();
                                }
                            });
                        } else {
                            // Snap backward cleanly down safely into standard resting state parameters
                            new com.replica.iosui.physics.IOSSpringAnimator(lockRoot).animateTo(currentTransY, 0f, val -> lockRoot.setTranslationY(val));
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void dismissLockScreen() {
        lockRoot.setVisibility(View.GONE);
        lockRoot.setTranslationY(0f);
        isLockVisible = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lockView != null) windowManager.removeView(lockView);
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
    }
}
