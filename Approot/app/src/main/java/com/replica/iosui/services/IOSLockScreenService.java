package com.replica.iosui.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.replica.iosui.R;
import com.replica.iosui.physics.IOSSpringAnimator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IOSLockScreenService extends Service {

    private WindowManager windowManager;
    private View lockView;
    private View lockRoot;
    private TextView clockText;
    private TextView dateText;
    private TextView swipeHintText;
    private BroadcastReceiver screenReceiver;
    
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    
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

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception ignored) {}

        lockView = LayoutInflater.from(this).inflate(R.layout.overlay_lockscreen, null);
        lockRoot = lockView.findViewById(R.id.lockRoot);
        clockText = lockView.findViewById(R.id.lockClockText);
        dateText = lockView.findViewById(R.id.lockDateText);
        
        // Find swipe text label component
        View hintGroup = lockView.findViewById(R.id.lockBottomHint);
        if (hintGroup instanceof android.view.ViewGroup) {
            View child = ((android.view.ViewGroup) hintGroup).getChildAt(0);
            if (child instanceof TextView) swipeHintText = (TextView) child;
        }

        // --- INJECT SF PRO TYPOGRAPHY PROGRAMMATICALLY ---
        try {
            Typeface sfProBold = ResourcesCompat.getFont(this, R.font.sf_pro_bold);
            Typeface sfProRegular = ResourcesCompat.getFont(this, R.font.sf_pro_regular);
            
            if (clockText != null) clockText.setTypeface(sfProBold);
            if (dateText != null) dateText.setTypeface(sfProRegular);
            if (swipeHintText != null) swipeHintText.setTypeface(sfProBold);
        } catch (Exception ignored) {}

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

        updateClockData();
        setupUnlockGesture();
        setupQuickActionButtons();
        setupScreenStateListeners();
    }

    private void updateClockData() {
        SimpleDateFormat clockFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        
        if (clockText != null) clockText.setText(clockFormat.format(new Date()));
        if (dateText != null) dateText.setText(dateFormat.format(new Date()).toUpperCase());
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
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private void showLockScreen() {
        if (!isLockVisible) {
            isLockVisible = true;
            lockRoot.setVisibility(View.VISIBLE);
            lockRoot.setAlpha(0f);
            new IOSSpringAnimator(lockRoot).animateTo(0f, 1f, val -> lockRoot.setAlpha(val));
        }
    }

    private void setupQuickActionButtons() {
        final ImageView torchBtn = lockView.findViewById(R.id.lockFlashlight);
        final ImageView cameraBtn = lockView.findViewById(R.id.lockCamera);

        torchBtn.setOnTouchListener(new View.OnTouchListener() {
            private float currentScale = 1.0f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        new IOSSpringAnimator(torchBtn).animateTo(currentScale, 1.3f, val -> {
                            currentScale = val;
                            torchBtn.setScaleX(val);
                            torchBtn.setScaleY(val);
                        });
                        return true;
                    case MotionEvent.ACTION_UP:
                        new IOSSpringAnimator(torchBtn).animateTo(currentScale, 1.0f, val -> {
                            currentScale = val;
                            torchBtn.setScaleX(val);
                            torchBtn.setScaleY(val);
                        });
                        if (cameraManager != null && cameraId != null) {
                            try {
                                isFlashlightOn = !isFlashlightOn;
                                cameraManager.setTorchMode(cameraId, isFlashlightOn);
                                torchBtn.setBackgroundResource(isFlashlightOn ? android.R.drawable.button_onoff_indicator_on : R.drawable.bg_ios_lock_button);
                            } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        new IOSSpringAnimator(torchBtn).animateTo(currentScale, 1.0f, val -> {
                            currentScale = val;
                            torchBtn.setScaleX(val);
                            torchBtn.setScaleY(val);
                        });
                        return true;
                }
                return false;
            }
        });

        cameraBtn.setOnTouchListener(new View.OnTouchListener() {
            private float currentScale = 1.0f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        new IOSSpringAnimator(cameraBtn).animateTo(currentScale, 1.3f, val -> {
                            currentScale = val;
                            cameraBtn.setScaleX(val);
                            cameraBtn.setScaleY(val);
                        });
                        return true;
                    case MotionEvent.ACTION_UP:
                        new IOSSpringAnimator(cameraBtn).animateTo(currentScale, 1.0f, val -> {
                            currentScale = val;
                            cameraBtn.setScaleX(val);
                            cameraBtn.setScaleY(val);
                        });
                        dismissLockScreen();
                        Intent cameraIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(cameraIntent);
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        new IOSSpringAnimator(cameraBtn).animateTo(currentScale, 1.0f, val -> {
                            currentScale = val;
                            cameraBtn.setScaleX(val);
                            cameraBtn.setScaleY(val);
                        });
                        return true;
                }
                return false;
            }
        });
    }

    private void setupUnlockGesture() {
        lockRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getY() > displayHeight - dpToPx(120)) return false;

                float rawY = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchY = rawY;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaY = rawY - initialTouchY;
                        if (deltaY < 0) lockRoot.setTranslationY(deltaY);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float currentTransY = lockRoot.getTranslationY();
                        if (currentTransY < -displayHeight * 0.30f) {
                      new IOSSpringAnimator(lockRoot).animateTo(currentTransY, -displayHeight, val -> {
lockRoot.setTranslationY(val);
if (val <= -displayHeight + 10f) dismissLockScreen();
});
} else {
new IOSSpringAnimator(lockRoot).animateTo(currentTransY, 0f, val -> lockRoot.setTranslationY(val));
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
private int dpToPx(int dp) {
return Math.round((float) dp * getResources().getDisplayMetrics().density);
}
@Override
public void onDestroy() {
super.onDestroy();
if (lockView != null) windowManager.removeView(lockView);
if (screenReceiver != null) unregisterReceiver(screenReceiver);
}
}
