package com.replica.iosui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.replica.iosui.physics.IOSSpringAnimator;
import com.replica.iosui.services.IOSLockScreenService;
import com.replica.iosui.services.IOSStatusBarService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout dotIndicator;
    private LinearLayout dockShelf;
    private PackageManager packageManager;
    private List<ResolveInfo> installedAppsList = new ArrayList<>();

    // Dynamic Island Service Binding Variables
    private IOSStatusBarService boundService;
    private boolean isServiceBound = false;

    // Service Connection Management Lifecycle Pipeline
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IOSStatusBarService.LocalBinder binder = (IOSStatusBarService.LocalBinder) service;
            boundService = binder.getService();
            isServiceBound = true;
            setupPositionSliders(); // Wire up SeekBars once communication is online
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        viewPager = findViewById(R.id.homeViewPager);
        dotIndicator = findViewById(R.id.pageIndicatorDots);
        dockShelf = findViewById(R.id.iosDockShelf);
        packageManager = getPackageManager();

        // 1. SPIN UP BACKGROUND OVERLAY SYSTEM MODULES
        Intent statusIntent = new Intent(this, IOSStatusBarService.class);
        startService(statusIntent);
        startService(new Intent(this, IOSLockScreenService.class));

        // 2. BIND SERVICE TO GAIN REAL-TIME OVERLAY COORDINATE CONTROL
        bindService(statusIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 3. FETCH INSTALLED DEVICE PACKAGES
        Intent startupIntent = new Intent(Intent.ACTION_MAIN, null);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        installedAppsList = packageManager.queryIntentActivities(startupIntent, 0);

        // 4. CONSTRUCT MULTI-PAGE DESKTOP VIEWPORT PIPELINES
        viewPager.setAdapter(new HomePagerAdapter());
        new TabLayoutMediator(dotIndicator, viewPager, (tab, position) -> {}).attach();

        // 5. INITIALISE PERSISTENT UTILITIES DECK SHORTCUTS
        setupDockShortcuts();

        // 6. INITIALISE DEFAULT APPS SELECTOR SWITCH UTILITY BUTTON
        // Make sure to add <Button android:id="@+id/btnToggleLauncher" ... /> inside your res/layout/activity_launcher.xml calibration box container
        View toggleBtn = findViewById(R.id.btnToggleLauncher);
        if (toggleBtn != null) {
            toggleBtn.setOnClickListener(v -> {
                Intent homeSelectorIntent = new Intent(Intent.ACTION_MAIN);
                homeSelectorIntent.addCategory(Intent.CATEGORY_HOME);
                homeSelectorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeSelectorIntent);
            });
        }
    }

    private void setupPositionSliders() {
        SeekBar seekX = findViewById(R.id.sliderX);
        SeekBar seekY = findViewById(R.id.sliderY);

        if (seekX != null && seekY != null) {
            seekX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (isServiceBound && boundService != null) {
                        boundService.updateIslandPosition(progress, seekY.getProgress());
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (isServiceBound && boundService != null) {
                        boundService.updateIslandPosition(seekX.getProgress(), progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void setupDockShortcuts() {
        String[] targetPackages = {
            "com.android.server.telecom",             // Native Dialer Component Handle
            "com.google.android.googlequicksearchbox", // Google Primary Search Core
            "com.google.android.apps.messaging",       // Google Messages Carrier
            "com.spotify.music"                       // Spotify Music Client
        };

        String[] alternatePackages = {
            "com.android.dialer",
            "com.android.contacts",
            "com.samsung.android.messaging",
            "com.android.mms"
        };

        for (String pkg : targetPackages) {
            Intent intent = packageManager.getLaunchIntentForPackage(pkg);
            
            if (intent == null) {
                for (String altPkg : alternatePackages) {
                    intent = packageManager.getLaunchIntentForPackage(altPkg);
                    if (intent != null) {
                        pkg = altPkg;
                        break;
                    }
                }
            }

            try {
                Drawable icon = (intent != null) ? packageManager.getApplicationIcon(pkg) : getWallpaper();
                View dockItem = createDockIconCard(icon, intent);
                if (dockShelf != null) {
                    dockShelf.addView(dockItem);
                }
            } catch (Exception ignored) {}
        }
    }

    private View createDockIconCard(Drawable iconDrawable, final Intent launchIntent) {
        View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, null, false);
        CardView frame = appCard.findViewById(R.id.iconCardFrame);
        ImageView iconImg = appCard.findViewById(R.id.appIconImage);
        TextView labelTxt = appCard.findViewById(R.id.appLabelText);

        if (labelTxt != null) labelTxt.setVisibility(View.GONE); // Hide text title on Dock bar icons
        if (iconImg != null) iconImg.setImageDrawable(iconDrawable);
        if (frame != null) frame.setRadius(Math.round(13 * getResources().getDisplayMetrics().density));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        appCard.setLayoutParams(params);

        appCard.setOnTouchListener(new View.OnTouchListener() {
            private float currentScale = 1.0f;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        new IOSSpringAnimator(appCard).animateTo(currentScale, 0.85f, val -> {
                            currentScale = val;
                            appCard.setScaleX(val);
                            appCard.setScaleY(val);
                        });
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        new IOSSpringAnimator(appCard).animateTo(currentScale, 1.0f, val -> {
                            currentScale = val;
                            appCard.setScaleX(val);
                            appCard.setScaleY(val);
                        });
                        if (event.getAction() == MotionEvent.ACTION_UP && launchIntent != null) {
                            startActivity(launchIntent);
                        }
                        return true;
                }
                return false;
            }
        });
        return appCard;
    }

    private View createAppIconCard(final ResolveInfo appInfo) {
        View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, null, false);
        CardView frame = appCard.findViewById(R.id.iconCardFrame);
        ImageView iconImg = appCard.findViewById(R.id.appIconImage);
        TextView labelTxt = appCard.findViewById(R.id.appLabelText);

        if (labelTxt != null) labelTxt.setText(appInfo.loadLabel(packageManager));
        if (iconImg != null) iconImg.setImageDrawable(appInfo.loadIcon(packageManager));
        if (frame != null) frame.setRadius(Math.round(13 * getResources().getDisplayMetrics().density));

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
case MotionEvent.ACTION_CANCEL:
new IOSSpringAnimator(appCard).animateTo(currentScale, 1.0f, val -> {
currentScale = val;
appCard.setScaleX(val);
appCard.setScaleY(val);
});
if (event.getAction() == MotionEvent.ACTION_UP) {
Intent intent = packageManager.getLaunchIntentForPackage(appInfo.activityInfo.packageName);
if (intent != null) startActivity(intent);
}
return true;
}
return false;
}
});
return appCard;
}
private class HomePagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
private static final int TYPE_GRID_PAGE = 0;
private static final int TYPE_LIBRARY_PAGE = 1;
private final int appsPerPage = 16;
@Override
public int getItemViewType(int position) {
int totalGridPages = (int) Math.ceil((double) installedAppsList.size() / appsPerPage);
return (position == totalGridPages) ? TYPE_LIBRARY_PAGE : TYPE_GRID_PAGE;
}
@NonNull
@Override
public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
if (viewType == TYPE_LIBRARY_PAGE) {
View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_app_library, parent, false);
return new LibraryViewHolder(view);
} else {
GridLayout gridLayout = new GridLayout(parent.getContext());
gridLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
gridLayout.setColumnCount(4);
gridLayout.setPadding(32, 32, 32, 32);
return new GridViewHolder(gridLayout);
}
}
@Override
public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
if (holder instanceof GridViewHolder) {
GridLayout grid = (GridLayout) holder.itemView;
grid.removeAllViews();
int startIndex = position * appsPerPage;
int endIndex = Math.min(startIndex + appsPerPage, installedAppsList.size());
for (int i = startIndex; i < endIndex; i++) {
grid.addView(createAppIconCard(installedAppsList.get(i)));
}
} else if (holder instanceof LibraryViewHolder) {
((LibraryViewHolder) holder).populateLibrary(installedAppsList);
}
}
@Override
public int getItemCount() {
return (int) Math.ceil((double) installedAppsList.size() / appsPerPage) + 1;
}
}
private class GridViewHolder extends RecyclerView.ViewHolder {
public GridViewHolder(@NonNull View v) { super(v); }
}
private class LibraryViewHolder extends RecyclerView.ViewHolder {
        private final GridLayout libraryGrid;
        private final EditText searchBar;
        public LibraryViewHolder(@NonNull View v) {
            super(v);
            libraryGrid = v.findViewById(R.id.libraryGrid);
            searchBar = v.findViewById(R.id.searchBarInput);
        }
        public void populateLibrary(List<ResolveInfo> allApps) {
            if (libraryGrid != null) {
                libraryGrid.removeAllViews();
                for (ResolveInfo app : allApps) libraryGrid.addView(createAppIconCard(app));
            }
            if (searchBar != null) {
                searchBar.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (libraryGrid != null) {
                            libraryGrid.removeAllViews();
                            for (ResolveInfo app : allApps) {
                                if (app.loadLabel(packageManager).toString().toLowerCase().contains(s.toString().toLowerCase())) {
                                    libraryGrid.addView(createAppIconCard(app));
                                }
                            }
                        }
                    }
                    @Override public void afterTextChanged(Editable s) {}
});
}
}
}
@Override
protected void onDestroy() {
super.onDestroy();
if (isServiceBound) {
unbindService(serviceConnection);
isServiceBound = false;
}
}
}
