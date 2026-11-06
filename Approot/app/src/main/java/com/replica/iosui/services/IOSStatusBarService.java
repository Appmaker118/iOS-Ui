package com.replica.iosui.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.replica.iosui.R;
import com.replica.iosui.views.IOSSliderView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class IOSStatusBarService extends Service {

    private WindowManager windowManager;
    private View masterView;
    
    // Core Layout Nodes
    private View ccRootView;
    private View statusBarView;
    private View hudContainer;
    private IOSSliderView hudSlider;
    private View dragTriggerView;

    // Orchestration Systems
    private Timer clockTimer;
    private AudioManager audioManager;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private CameraManager cameraManager;
    private MediaSessionManager mediaSessionManager;
    private MediaController activeMediaController;
    private BroadcastReceiver volumeReceiver;
    private BroadcastReceiver bluetoothReceiver;
    
    // State Framework Pointers
    private String cameraId;
    private boolean isFlashlightOn = false;
    private boolean isCCVisible = false;
    private boolean isHudVisible = false;
    private boolean isHudExpanded = false;
    
    private float initialTouchY = 0f;
    private float hudWidth = 14f;
    private int displayHeight = 0;
    
    private Handler hudDismissHandler;
    private Runnable dismissRunnable;
    
    private WindowManager.LayoutParams baseParams;
    private WindowManager.LayoutParams dragParams;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        displayHeight = getResources().getDisplayMetrics().heightPixels;
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        hudDismissHandler = new Handler(Looper.getMainLooper());

        // 1. HARDWARE SYSTEM INITIALIZERS
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm != null) bluetoothAdapter = bm.getAdapter();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null && cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception ignored) {}

        // 2. INFLATE MASTER INTERFACE CANVAS
        masterView = LayoutInflater.from(this).inflate(R.layout.overlay_control_centre, null);
        ccRootView = masterView.findViewById(R.id.ccRoot);
        statusBarView = masterView.findViewById(R.id.statusBarContainer);
        hudContainer = masterView.findViewById(R.id.hudContainer);
        hudSlider = masterView.findViewById(R.id.hudSlider);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE;

        baseParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        baseParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(masterView, baseParams);

        // 3. CREATE INVISIBLE INTERCEPT AREA FOR DRAG GESTURES
        dragParams = new WindowManager.LayoutParams(
                displayWidth / 3,
                dpToPx(44),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        dragParams.gravity = Gravity.TOP | Gravity.END;
        dragTriggerView = new View(this);
        dragTriggerView.setBackgroundColor(android.graphics.Color.TRANSPAREMENT);
        windowManager.addView(dragTriggerView, dragParams);

        // 4. INITIALIZE FUNCTIONAL LOOPS
        initClockEngine();
        initVolumeHUDSystem();
        initDragAndHardwareUtilities();
        initMediaPlaybackEngine();
    }

    private void initClockEngine() {
        final TextView timeTextView = masterView.findViewById(R.id.statusBarTime);
        final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm", Locale.getDefault());
        clockTimer = new Timer();
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    timeTextView.setText(timeFormatter.format(new Date()));
                });
            }
        }, 0, 1000);
    }

    private void initVolumeHUDSystem() {
        dismissRunnable = () -> hideVolumeHUD();
        
        hudSlider.setOnTouchListener((v, event) -> {
            if (!isHudExpanded) {
                isHudExpanded = true;
                hudDismissHandler.removeCallbacks(dismissRunnable);
                new com.replica.iosui.physics.IOSSpringAnimator(hudSlider).animateTo(hudWidth, 48f, val -> {
                    hudWidth = val;
                    hudSlider.getLayoutParams().width = dpToPx((int) val);
                    hudSlider.requestLayout();
                });
            }
            return false;
        });

        volumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                    triggerVolumeHUDUpdate();
                }
            }
        };
        registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
    }

    private void triggerVolumeHUDUpdate() {
        hudDismissHandler.removeCallbacks(dismissRunnable);
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        hudSlider.setProgress((float) currentVol / (float) maxVol);

        if (!isHudVisible) {
            isHudVisible = true;
            hudContainer.setVisibility(View.VISIBLE);
            hudContainer.setTranslationX(-dpToPx(40));
            new com.replica.iosui.physics.IOSSpringAnimator(hudContainer).animateTo(-dpToPx(40), 0f, val -> hudContainer.setTranslationX(val));
        }
        hudDismissHandler.postDelayed(dismissRunnable, 2000);
    }

    private void hideVolumeHUD() {
        if (isHudVisible) {
            new com.replica.iosui.physics.IOSSpringAnimator(hudContainer).animateTo(hudContainer.getTranslationX(), -dpToPx(60), val -> {
                hudContainer.setTranslationX(val);
                if (val <= -dpToPx(58)) {
                    isHudVisible = false;
                    isHudExpanded = false;
                    hudContainer.setVisibility(View.GONE);
                    hudWidth = 14f;
                    hudSlider.getLayoutParams().width = dpToPx(14);
                    hudSlider.requestLayout();
                }
            });
        }
    }

    private void initDragAndHardwareUtilities() {
        ImageView wifiBtn = masterView.findViewById(R.id.ccWifi);
        ImageView bluetoothBtn = masterView.findViewById(R.id.ccBluetooth);
        ImageView torchBtn = masterView.findViewById(R.id.ccFlashlight);
        View mirrorBtn = masterView.findViewById(R.id.ccWifi).getRootView().findViewById(R.id.ccTrackTitle).getRootView(); 
        
        com.replica.iosui.views.IOSSliderView brightSlider = masterView.findViewById(R.id.ccBrightness);
        com.replica.iosui.views.IOSSliderView volumeSlider = masterView.findViewById(R.id.ccVolume);

        // --- 1. WI-FI UTILITY ---
        if (wifiManager != null) wifiBtn.setBackgroundColor(wifiManager.isWifiEnabled() ? 0xFF007AFF : 0xFF444444);
        wifiBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(panelIntent);
            } else if (wifiManager != null) {
                boolean state = !wifiManager.isWifiEnabled();
                wifiManager.setWifiEnabled(state);
                wifiBtn.setBackgroundColor(state ? 0xFF007AFF : 0xFF444444);
            }
        });

        // --- 2. BLUETOOTH UTILITY ---

        if (bluetoothAdapter != null) bluetoothBtn.setBackgroundColor(bluetoothAdapter.isEnabled() ? 0xFF007AFF : 0xFF444444);
bluetoothBtn.setOnClickListener(v -> {
if (bluetoothAdapter != null) {
if (bluetoothAdapter.isEnabled()) {
bluetoothAdapter.disable();
bluetoothBtn.setBackgroundColor(0xFF444444);
} else {
Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(enableBtIntent);
}
}
});
bluetoothReceiver = new BroadcastReceiver() {
@Override
public void onReceive(Context context, Intent intent) {
int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
if (state == BluetoothAdapter.STATE_ON) bluetoothBtn.setBackgroundColor(0xFF007AFF);
if (state == BluetoothAdapter.STATE_OFF) bluetoothBtn.setBackgroundColor(0xFF444444);
}
};
registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
// --- 3. FLASHLIGHT UTILITY ---
torchBtn.setOnClickListener(v -> {
if (cameraManager != null && cameraId != null) {
try {
isFlashlightOn = !isFlashlightOn;
cameraManager.setTorchMode(cameraId, isFlashlightOn);
torchBtn.setBackgroundColor(isFlashlightOn ? 0xFF007AFF : 0xFF444444);
} catch (Exception ignored) {}
}
});
// --- 4. SCREEN MIRRORING SHORTCUT ---
// Instantly hooks to the Android Cast dialog utility pipeline layout
View mirrorContainer = masterView.findViewById(R.id.ccVolume).getRootView();
View mirroringTargetLayout = mirrorContainer.findViewWithTag("mirrorTarget");
if (mirroringTargetLayout != null) {
mirroringTargetLayout.setOnClickListener(v -> {
Intent castIntent = new Intent(Settings.ACTION_CAST_SETTINGS);
castIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(castIntent);
});
}
// --- 5. SYSTEM AUDIO & SLIDERS ---
try {
brightSlider.setProgress((float) Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255f);
} catch (Exception ignored) {}
brightSlider.setOnTouchListener((v, event) -> {
int val = Math.round(brightSlider.getProgress() * 255f);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, val);
}
return false;
});
int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
volumeSlider.setProgress((float) currentVol / (float) maxVol);
volumeSlider.setOnTouchListener((v, event) -> {
int targetVol = Math.round(volumeSlider.getProgress() * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);
return false;
});
// --- 6. DRAG MECHANICS EDGE TRACKER ---
dragTriggerView.setOnTouchListener((v, event) -> {
float rawY = event.getRawY();
switch (event.getAction()) {
case MotionEvent.ACTION_DOWN:
initialTouchY = rawY;
if (!isCCVisible) {
isCCVisible = true;
ccRootView.setVisibility(View.VISIBLE);
ccRootView.setTranslationY(-displayHeight);
updateWindowFlags(true);
}
return true;
case MotionEvent.ACTION_MOVE:
float deltaY = rawY - initialTouchY;
if (deltaY > 0) ccRootView.setTranslationY(Math.min(0f, -displayHeight + deltaY));
return true;
case MotionEvent.ACTION_UP:
case MotionEvent.ACTION_CANCEL:
float currentTrans = ccRootView.getTranslationY();
if (currentTrans > -displayHeight * 0.75f) {
new com.replica.iosui.physics.IOSSpringAnimator(ccRootView).animateTo(currentTrans, 0f, val -> ccRootView.setTranslationY(val));
} else {
new com.replica.iosui.physics.IOSSpringAnimator(ccRootView).animateTo(currentTrans, -displayHeight, val -> {
ccRootView.setTranslationY(val);
if (val <= -displayHeight + 5f) {
isCCVisible = false;
ccRootView.setVisibility(View.GONE);
updateWindowFlags(false);
}
});
}
return true;
}
return false;
});
}
// --- 7. NOW PLAYING MEDIA HARDWARE SYNCHRONIZATION ---
private void initMediaPlaybackEngine() {
final TextView trackTitle = masterView.findViewById(R.id.ccTrackTitle);
final ImageView playPauseBtn = masterView.findViewById(R.id.ccMediaPlayPause);
if (mediaSessionManager != null) {
try {
List controllers = mediaSessionManager.getActiveSessions(null);
if (!controllers.isEmpty()) {
activeMediaController = controllers.get(0);
updateMediaUI(trackTitle, playPauseBtn);
}
} catch (Exception ignored) {
trackTitle.setText("Not Playing");
}
}
playPauseBtn.setOnClickListener(v -> {
if (activeMediaController != null) {
PlaybackState pbState = activeMediaController.getPlaybackState();
if (pbState != null && pbState.getState() == PlaybackState.STATE_PLAYING) {
activeMediaController.getTransportControls().pause();
playPauseBtn.setImageResource(android.graphics.drawable.ic_media_play);
} else if (activeMediaController != null) {
activeMediaController.getTransportControls().play();
playPauseBtn.setImageResource(android.graphics.drawable.ic_media_pause);
}
}
});
}
private void updateMediaUI(TextView title, ImageView button) {
if (activeMediaController != null && activeMediaController.getMetadata() != null) {
String name = activeMediaController.getMetadata().getString(MediaMetadata.METADATA_TITLE);
title.setText(name != null ? name : "Unknown Track");
}
if (activeMediaController != null && activeMediaController.getPlaybackState() != null) {
boolean playing = activeMediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
button.setImageResource(playing ? android.graphics.drawable.ic_media_pause : android.graphics.drawable.ic_media_play);
}
}
private void updateWindowFlags(boolean focusable) {
if (focusable) {
baseParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
} else {
baseParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
}
windowManager.updateViewLayout(masterView, baseParams);
}
private int dpToPx(int dp) {
return Math.round((float) dp * getResources().getDisplayMetrics().density);
}
@Override
public void onDestroy() {
super.onDestroy();
if (masterView != null) windowManager.removeView(masterView);
if (dragTriggerView != null) windowManager.removeView(dragTriggerView);
if (clockTimer != null) clockTimer.cancel();
if (volumeReceiver != null) unregisterReceiver(volumeReceiver);
if (bluetoothReceiver != null) unregisterReceiver(bluetoothReceiver);
}
}
