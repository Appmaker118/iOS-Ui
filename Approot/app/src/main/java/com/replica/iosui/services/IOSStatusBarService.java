// Append these imports to the top of your file
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import com.replica.iosui.views.IOSSliderView;

// Add these private variables inside the main class block:
private WindowManager.LayoutParams hudParams;
private View volumeHudView;
private IOSSliderView hudSlider;
private View hudContainer;
private AudioManager audioManager;
private VolumeKeyReceiver volumeReceiver;
private Handler hudDismissHandler;
private Runnable dismissRunnable;

private boolean isHudVisible = false;
private boolean isExpanded = false;
private float hudWidth = 14f; // Track active base layout dimensions
    // Initialize standard Android core audio hooks
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    hudDismissHandler = new Handler(Looper.getMainLooper());

    setupVolumeHUDWindow();
    
    // Wire up tracking behaviors directly to hardware audio stream updates
    volumeReceiver = new VolumeKeyReceiver(new VolumeKeyReceiver.VolumeChangeListener() {
        @Override
        public void onVolumeChanged() {
            triggerVolumeHudUpdate();
        }
    });
    
    IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
    registerReceiver(volumeReceiver, filter);
    private void setupVolumeHUDWindow() {
        volumeHudView = LayoutInflater.from(this).inflate(R.layout.overlay_volume_hud, null);
        hudSlider = volumeHudView.findViewById(R.id.hudSlider);
        hudContainer = volumeHudView.findViewById(R.id.hudContainer);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE;

        hudParams = new WindowManager.LayoutParams(
                dpToPx(100),
                dpToPx(220),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        // Position it exactly on the left side of the screen matching modern iOS device standards
        hudParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        
        // Initialize dismiss task thread routines
        dismissRunnable = () -> hideVolumeHUD();

        // Wire touch listeners to seamlessly convert the thin indicator line into a thick slider
        hudSlider.setOnTouchListener((v, event) -> {
            if (!isExpanded) {
                isExpanded = true;
                hudDismissHandler.removeCallbacks(dismissRunnable);
                
                // Dynamic spring transition tracking: widening the tap frame canvas bounds
                new com.replica.iosui.physics.IOSSpringAnimator(hudSlider).animateTo(hudWidth, 48f, val -> {
                    hudWidth = val;
                    hudSlider.getLayoutParams().width = dpToPx((int) val);
                    hudSlider.requestLayout();
                });
            }
            return false; // Allow touch pass-through to handle custom baseline tracking math safely
        });
    }

    private void triggerVolumeHudUpdate() {
        hudDismissHandler.removeCallbacks(dismissRunnable);

        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float currentRatio = (float) currentVol / (float) maxVol;

        hudSlider.setProgress(currentRatio);

        if (!isHudVisible) {
            isHudVisible = true;
            windowManager.addView(volumeHudView, hudParams);
            
            // Pop slider container out from extreme edge boundary locations dynamically
            hudContainer.setTranslationX(-dpToPx(40));
            new com.replica.iosui.physics.IOSSpringAnimator(hudContainer).animateTo(-dpToPx(40), 0f, val -> {
                hudContainer.setTranslationX(val);
            });
        }

        // Keep it visible for 2 seconds before sliding away unless actively manipulated by the user
        hudDismissHandler.postDelayed(dismissRunnable, 2000);
    }

    private void hideVolumeHUD() {
        if (isHudVisible) {
            // Animate it clean out off-canvas gracefully before fully tearing layout frame variables down
            new com.replica.iosui.physics.IOSSpringAnimator(hudContainer).animateTo(hudContainer.getTranslationX(), -dpToPx(60), val -> {
                hudContainer.setTranslationX(val);
                if (val <= -dpToPx(58)) {
                    if (isHudVisible) {
                        windowManager.removeView(volumeHudView);
                        isHudVisible = false;
                        isExpanded = false;
                        
                        // Reset slider scale parameters back down safely to base structural states
                        hudWidth = 14f;
                        hudSlider.getLayoutParams().width = dpToPx(14);
                        hudSlider.requestLayout();
                    }
                }
            });
        }
    }
