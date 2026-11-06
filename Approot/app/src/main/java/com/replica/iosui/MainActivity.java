package com.replica.iosui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        viewPager = findViewById(R.id.homeViewPager);
        dotIndicator = findViewById(R.id.pageIndicatorDots);
        dockShelf = findViewById(R.id.iosDockShelf);
        packageManager = getPackageManager();

        startService(new Intent(this, IOSStatusBarService.class));
        startService(new Intent(this, IOSLockScreenService.class));

        // Gather system packages
        Intent startupIntent = new Intent(Intent.ACTION_MAIN, null);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        installedAppsList = packageManager.queryIntentActivities(startupIntent, 0);

        viewPager.setAdapter(new HomePagerAdapter());
        new TabLayoutMediator(dotIndicator, viewPager, (tab, position) -> {}).attach();

        // INJECT UTILITIES INTO DOCK SHELF GRID
        setupDockShortcuts();
    }

    private void setupDockShortcuts() {
        // Define targeted operational system handles
        String[] targetPackages = {
            "com.android.server.telecom", // Native Dialler/Phone core indicator fallback
            "com.google.android.googlequicksearchbox", // Google
            "com.google.android.apps.messaging", // Google Messages
            "com.spotify.music" // Spotify
        };

        // Alternate standard package variations common among modern devices
        String[] alternatePackages = {
            "com.android.dialer",
            "com.android.contacts",
            "com.samsung.android.messaging",
            "com.android.mms"
        };

        for (String pkg : targetPackages) {
            Intent intent = packageManager.getLaunchIntentForPackage(pkg);
            
            // Try matching alternate vendor app structures if standard names aren't present
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
                String label = (intent != null) ? packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() : "App";
                
                View dockItem = createDockIconCard(icon, intent);
                dockShelf.addView(dockItem);
            } catch (Exception ignored) {}
        }
    }

    private View createDockIconCard(Drawable iconDrawable, final Intent launchIntent) {
        View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, null, false);
        CardView frame = appCard.findViewById(R.id.iconCardFrame);
        ImageView iconImg = appCard.findViewById(R.id.appIconImage);
        TextView labelTxt = appCard.findViewById(R.id.appLabelText);

        // Hide description text tags entirely matching clean native iOS baseline standards
        labelTxt.setVisibility(View.GONE); 
        iconImg.setImageDrawable(iconDrawable);
        frame.setRadius(Math.round(13 * getResources().getDisplayMetrics().density));

        // Adjust dimensions to fit cleanly within the layout shelf boundaries
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

    // --- REUSE HORIZONTAL WORKSPACE MULTI-PAGE VIEW ADAPTERS FROM STEP 13 ---
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

    private View createAppIconCard(final ResolveInfo appInfo) {
        View appCard = LayoutInflater.from(this).inflate(R.layout.item_ios_app, null, false);
        CardView frame = appCard.findViewById(R.id.iconCardFrame);
        ImageView iconImg = appCard.findViewById(R.id.appIconImage);
        TextView labelTxt = appCard.findViewById(R.id.appLabelText);

        labelTxt.setText(appInfo.loadLabel(packageManager));
        iconImg.setImageDrawable(appInfo.loadIcon(packageManager));
        frame.setRadius(Math.round(13 * getResources().getDisplayMetrics().density));

        appCard.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = packageManager.getLaunchIntentForPackage(appInfo.activityInfo.packageName);
                if (intent != null) startActivity(intent);
            }
            return true;
        });
        return appCard;
    }

    private class GridViewHolder extends RecyclerView.ViewHolder { public GridViewHolder(@NonNull View v) { super(v); } }
    private class LibraryViewHolder extends RecyclerView.ViewHolder {
        private final GridLayout libraryGrid;
        private final EditText searchBar;
        public LibraryViewHolder(@NonNull View v) {
            super(v);
            libraryGrid = v.findViewById(R.id.libraryGrid);
            searchBar = v.findViewById(R.id.searchBarInput);
            }
public void populateLibrary(List allApps) {
libraryGrid.removeAllViews();
for (ResolveInfo app : allApps) libraryGrid.addView(createAppIconCard(app));
searchBar.addTextChangedListener(new TextWatcher() {
@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
libraryGrid.removeAllViews();
for (ResolveInfo app : allApps) {
if (app.loadLabel(packageManager).toString().toLowerCase().contains(s.toString().toLowerCase())) {
libraryGrid.addView(createAppIconCard(app));
}
}
}
@Override public void afterTextChanged(Editable s) {}
});
}
}
}
