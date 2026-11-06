package com.replica.iosui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.replica.iosui.physics.IOSSpringAnimator;
import com.replica.iosui.services.IOSLockScreenService;
import com.replica.iosui.services.IOSStatusBarService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridLayout appsGridLayout;
    private PackageManager packageManager;
    
    // Edit Mode State Variables
    private boolean isEditModeActive = false;
    private final List<View> appCardViewsList = new ArrayList<>();
    private final List<ObjectAnimator> activeJiggleAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        appsGridLayout = findViewById(R.id.appsGrid);
        packageManager = getPackageManager();

        startService(new Intent(this, IOSStatusBarService.class));
        startService(new Intent(this, IOSLockScreenService.class));

        loadDeviceApplications();

        // Tapping the background blank desk sheet instantly cancels active editing
        findViewById(R.id.launcherWallpaper).setOnClickListener(v -> {
            if (isEditModeActive) {
                exitEditJiggleMode();
            }
        });
    }

    private void loadDeviceApplications() {
        Intent startupIntent = new Intent(Intent.ACTION_MAIN, null);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableActivities = packageManager.queryIntentActivities(startupIntent, 0);

        for (final ResolveInfo appInfo : availableActivities) {
            final View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, appsGridLayout, false);
            
            CardView frame = appCard.findViewById(R.id.iconCardFrame);
            ImageView iconImg = appCard.findViewById(R.id.appIconImage);
            TextView labelTxt = appCard.findViewById(R.id.appLabelText);
            final View deleteBadge = appCard.findViewById(R.id.deleteBadge);

            labelTxt.setText(appInfo.loadLabel(packageManager));
            iconImg.setImageDrawable(appInfo.loadIcon(packageManager));
            frame.setRadius(dpToPx(13)); 

            appCardViewsList.add(appCard);

            // --- WIRE LONG PRESS EVENT TO START EDIT MODE ---
            appCard.setOnLongClickListener(v -> {
                if (!isEditModeActive) {
                    enterEditJiggleMode();
                }
                return true;
            });

            // --- TOUCH LOGIC COMPATIBLE WITH ACTIVE JIGGLE MODES ---
            appCard.setOnTouchListener(new View.OnTouchListener() {
                private float currentScale = 1.0f;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isEditModeActive) return false; // Bypass scale shifts when editing

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            new IOSSpringAnimator(appCard).animateTo(currentScale, 0.88f, val -> {
                                currentScale = val;
                                appCard.setScaleX(val);
                                appCard.setScaleY(val);
                            });
                            return false; // Return false so LongClickListener can still trigger

                        case MotionEvent.ACTION_UP:
                            new IOSSpringAnimator(appCard).animateTo(currentScale, 1.0f, val -> {
                                currentScale = val;
                                appCard.setScaleX(val);
                                appCard.setScaleY(val);
                            });
                            
                            Intent launchIntent = packageManager.getLaunchIntentForPackage(appInfo.activityInfo.packageName);
                            if (launchIntent != null) {
                                startActivity(launchIntent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            }
                            return true;

                        case MotionEvent.ACTION_CANCEL:
                            new IOSSpringAnimator(appCard).animateTo(currentScale, 1.0f, val -> {
                                currentScale = val;
                                appCard.setScaleX(val);
                                appCard.setScaleY(val);
                            });
                            return true;
                    }
                    return false;
                }
            });

            // Delete button shortcut behavior template hook
            deleteBadge.setOnClickListener(v -> {
                appsGridLayout.removeView(appCard);
                appCardViewsList.remove(appCard);
            });

            appsGridLayout.addView(appCard);
        }
    }

    private void enterEditJiggleMode() {
        isEditModeActive = true;
        
        // Randomly offset the initial start point of each icon's animation so they don't jiggle perfectly in sync
        java.util.Random rand = new java.util.Random();

        for (View card : appCardViewsList) {
            View badge = card.findViewById(R.id.deleteBadge);
            badge.setVisibility(View.VISIBLE);
            
            // Pop badge into view cleanly with a mini bounce scale effect
            badge.setScaleX(0f); badge.setScaleY(0f);
            new IOSSpringAnimator(badge).animateTo(0f, 1f, val -> {
                badge.setScaleX(val);
                badge.setScaleY(val);
            });

            // Set up the rapid back-and-forth rotational jiggle loop
            ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(card, "rotation", -1.8f, 1.8f);
            rotateAnim.setDuration(120 + rand.nextInt(40)); // Alternate speeds slightly
            rotateAnim.setRepeatMode(ValueAnimator.REVERSE);
            rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnim.start();
            
            activeJiggleAnimators.add(rotateAnim);
        }
    }

    private void exitEditJiggleMode() {
        isEditModeActive = false;

        // Stop all active rotation loops instantly
        for (ObjectAnimator anim : activeJiggleAnimators) {
            anim.cancel();
        }
        activeJiggleAnimators.clear();

        // Smoothly snap all icons back to zero rotation and hide the delete badges
        for (View card : appCardViewsList) {
            card.setRotation(0f);
            final View badge = card.findViewById(R.id.deleteBadge);
            
            new IOSSpringAnimator(badge).animateTo(badge.getScaleX(), 0f, val -> {
                badge.setScaleX(val);
                badge.setScaleY(val);
                if (val <= 0.05f) badge.setVisibility(View.GONE);
            });
        }
    }

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (isEditModeActive) {
            exitEditJiggleMode();
        } else {
            super.onBackPressed();
        }
    }
}
