package com.replica.iosui;

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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridLayout appsGridLayout;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        appsGridLayout = findViewById(R.id.appsGrid);
        packageManager = getPackageManager();

        // 1. BOOT BACKGROUND SYSTEM SERVICE LAYERS
        startService(new Intent(this, IOSStatusBarService.class));
        startService(new Intent(this, IOSLockScreenService.class));

        // 2. CONSTRUCT INSTALLED APPLICATION SECTOR INTERFACES
        loadDeviceApplications();
    }

    private void loadDeviceApplications() {
        Intent startupIntent = new Intent(Intent.ACTION_MAIN, null);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Fetch application pointer structures via native package managers safely
        List<ResolveInfo> availableActivities = packageManager.queryIntentActivities(startupIntent, 0);

        for (final ResolveInfo appInfo : availableActivities) {
            View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, appsGridLayout, false);
            
            CardView frame = appCard.findViewById(R.id.iconCardFrame);
            ImageView iconImg = appCard.findViewById(R.id.appIconImage);
            TextView labelTxt = appCard.findViewById(R.id.appLabelText);

            // Fetch app parameters
            CharSequence nameLabel = appInfo.loadLabel(packageManager);
            Drawable iconDrawable = appInfo.loadIcon(packageManager);
            final String targetPackage = appInfo.activityInfo.packageName;

            labelTxt.setText(nameLabel);
            iconImg.setImageDrawable(iconDrawable);

            // Apply exact mathematical 22% corner radius properties to match iOS design
            frame.setRadius(dpToPx(13)); 

            // Wire up the physics scale-down press effect when tapping an icon
            appCard.setOnTouchListener(new View.OnTouchListener() {
                private float currentScale = 1.0f;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            new IOSSpringAnimator(appCard).animateTo(currentScale, 0.88f, val -> {
                                currentScale = val;
                                appCard.setScaleX(val);
                                appCard.setScaleY(val);
                            });
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            new IOSSpringAnimator(appCard).animateTo(currentScale, 1.0f, val -> {
                                currentScale = val;
                                appCard.setScaleX(val);
                                appCard.setScaleY(val);
                            });
                            
                            // Smoothly launch the selected app
                            Intent launchIntent = packageManager.getLaunchIntentForPackage(targetPackage);
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

            // Inject the configured app card view directly into the layout workspace grid
            appsGridLayout.addView(appCard);
        }
    }

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }
}
