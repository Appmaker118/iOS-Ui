package com.replica.iosui.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.replica.iosui.R;

public class IOSStatusBarService extends Service {

    private WindowManager windowManager;
    private View statusBarView;
    private Timer clockTimer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        statusBarView = LayoutInflater.from(this).inflate(R.layout.overlay_status_bar, null);

        // Define window parameters for top-level global injection
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dpToPx(44), // iOS Standard native height tracking
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        
        // Inject layout onto the device canvas
        windowManager.addView(statusBarView, params);

        // Start dynamic clock synchronization loops
        initClockEngine();
    }

    private void initClockEngine() {
        final TextView timeTextView = statusBarView.findViewById(R.id.statusBarTime);
        final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm", Locale.getDefault());
        
        clockTimer = new Timer();
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Post calculations back to main view thread
                statusBarView.post(() -> {
                    String currentTime = timeFormatter.format(new Date());
                    timeTextView.setText(currentTime);
                });
            }
        }, 0, 1000); // Check and refresh text variables every second
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (statusBarView != null && windowManager != null) {
            windowManager.removeView(statusBarView);
        }
        if (clockTimer != null) {
            clockTimer.cancel();
        }
    }
}

