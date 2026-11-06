package com.replica.iosui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
    private PackageManager packageManager;
    private List<ResolveInfo> installedAppsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        viewPager = findViewById(R.id.homeViewPager);
        dotIndicator = findViewById(R.id.pageIndicatorDots);
        packageManager = getPackageManager();

        startService(new Intent(this, IOSStatusBarService.class));
        startService(new Intent(this, IOSLockScreenService.class));

        // Load hardware apps
        Intent startupIntent = new Intent(Intent.ACTION_MAIN, null);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        installedAppsList = packageManager.queryIntentActivities(startupIntent, 0);

        // Configure multi-page workspace adapter pipelines
        viewPager.setAdapter(new HomePagerAdapter());
        new TabLayoutMediator(dotIndicator, viewPager, (tab, position) -> {}).attach();
    }

    private class HomePagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_GRID_PAGE = 0;
        private static final int TYPE_LIBRARY_PAGE = 1;
        private final int appsPerPage = 16; // 4x4 matrix grid sub-allotment allocations per page

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
                LibraryViewHolder libHolder = (LibraryViewHolder) holder;
                libHolder.populateLibrary(installedAppsList);
            }
        }

        @Override
        public int getItemCount() {
            int totalGridPages = (int) Math.ceil((double) installedAppsList.size() / appsPerPage);
            return totalGridPages + 1; // Append the App Library page as the final option
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

    private class GridViewHolder extends RecyclerView.ViewHolder {
        public GridViewHolder(@NonNull View itemView) { super(itemView); }
    }

    private class LibraryViewHolder extends RecyclerView.ViewHolder {
        private final GridLayout libraryGrid;
        private final EditText searchBar;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            libraryGrid = itemView.findViewById(R.id.libraryGrid);
            searchBar = itemView.findViewById(R.id.searchBarInput);
        }

        public void populateLibrary(final List<ResolveInfo> allApps) {
            renderGridItems(allApps);
            
            // TextWatcher loop to handle input filtering dynamically on every keypress
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    List<ResolveInfo> filteredApps = new ArrayList<>();
                    for (ResolveInfo app : allApps) {
                        if (app.loadLabel(packageManager).toString().toLowerCase().contains(s.toString().toLowerCase())) {
                            filteredApps.add(app);
                        }
                    }
                    renderGridItems(filteredApps);
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        private void renderGridItems(List<ResolveInfo> apps) {
            libraryGrid.removeAllViews();
            for (ResolveInfo app : apps) {
                libraryGrid.addView(createAppIconCard(app));
            }
        }
    }
}
