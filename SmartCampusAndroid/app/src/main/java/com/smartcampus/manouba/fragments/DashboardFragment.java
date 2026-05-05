package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.ImageCarouselAdapter;
import com.smartcampus.manouba.adapters.StatsAdapter;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private ViewPager2 viewPagerCarousel;
    private LinearLayout carouselIndicators;
    private RecyclerView rvStats;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private int currentCarouselPage = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        autoScrollHandler = new Handler(Looper.getMainLooper());

        viewPagerCarousel = view.findViewById(R.id.viewpager_carousel);
        carouselIndicators = view.findViewById(R.id.layout_carousel_indicators);
        rvStats = view.findViewById(R.id.rv_stats);

        setupCarousel();
        setupStats();
        setupQuickActions(view);

        // Try to load from API, but fallback to static data if it fails
        try {
            loadDataFromApi();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCarousel() {
        int[] images = {
                R.drawable.uma,
                R.drawable.image2,
                R.drawable.image44,
                R.drawable.image33,
                R.drawable.greenspaces
        };
        String[] captions = {
                "Main Campus Entrance", "Modern Learning Facilities",
                "Student Life", "Innovation & Technology", "Campus Green Spaces"
        };

        ImageCarouselAdapter adapter = new ImageCarouselAdapter(images, captions);
        viewPagerCarousel.setAdapter(adapter);

        setupCarouselIndicators(images.length);

        viewPagerCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentCarouselPage = position;
                updateCarouselIndicators(position, images.length);
            }
        });

        // Auto-advance every 5 seconds
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || viewPagerCarousel == null || viewPagerCarousel.getAdapter() == null) return;
                int count = viewPagerCarousel.getAdapter().getItemCount();
                if (count > 0) {
                    currentCarouselPage = (currentCarouselPage + 1) % count;
                    viewPagerCarousel.setCurrentItem(currentCarouselPage, true);
                }
                autoScrollHandler.postDelayed(this, 5000);
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 5000);
    }

    private void setupCarouselIndicators(int count) {
        carouselIndicators.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            if (i == 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size * 3, size);
                params.setMargins(size / 2, 0, size / 2, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.shape_indicator_active);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(size / 2, 0, size / 2, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.shape_indicator);
            }
            carouselIndicators.addView(dot);
        }
    }

    private void updateCarouselIndicators(int position, int count) {
        if (carouselIndicators == null) return;
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        for (int i = 0; i < count && i < carouselIndicators.getChildCount(); i++) {
            View dot = carouselIndicators.getChildAt(i);
            if (dot == null) continue;
            if (i == position) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size * 3, size);
                params.setMargins(size / 2, 0, size / 2, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.shape_indicator_active);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(size / 2, 0, size / 2, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.shape_indicator);
            }
        }
    }

    private void setupStats() {
        List<String[]> stats = new ArrayList<>();
        stats.add(new String[]{"people", "18,601", "Students"});
        stats.add(new String[]{"school", "1,354", "Teachers"});
        stats.add(new String[]{"book", "147", "Programs"});
        stats.add(new String[]{"business", "16+", "Institutions"});
        stats.add(new String[]{"globe", "296", "International"});
        stats.add(new String[]{"flask", "31", "Research"});

        rvStats.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvStats.setAdapter(new StatsAdapter(stats));
        rvStats.setNestedScrollingEnabled(false);
    }

    private void setupQuickActions(View view) {
        View btnAbout = view.findViewById(R.id.btn_about_campus);
        View btnReport = view.findViewById(R.id.btn_report_issue);

        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                try {
                    View dialogView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.dialog_about_campus, null);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setView(dialogView)
                            .setPositiveButton("Close", null)
                            .show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> showReportDialog());
        }
    }

    private void showReportDialog() {
        String[] issueTypes = {"Facilities & Infrastructure", "Accessibility",
                "Safety & Security", "Technology & IT", "Other"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Report an Issue")
                .setSingleChoiceItems(issueTypes, 0, (d, which) -> {})
                .setPositiveButton("Submit", (d, which) -> {
                    Toast.makeText(requireContext(), R.string.report_success, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDataFromApi() {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        if (token == null) return;

        try {
            RetrofitClient.getInstance(token).getApi().getStats().enqueue(new Callback<List<JsonObject>>() {
                @Override
                public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        List<String[]> apiStats = new ArrayList<>();
                        for (JsonObject stat : response.body()) {
                            String icon = stat.has("icon") ? stat.get("icon").getAsString() : "people";
                            String value = stat.has("value") ? stat.get("value").getAsString() : "0";
                            String label = stat.has("label") ? stat.get("label").getAsString() : "";
                            apiStats.add(new String[]{icon, value, label});
                        }
                        if (rvStats != null) rvStats.setAdapter(new StatsAdapter(apiStats));
                    }
                }

                @Override
                public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                    // Keep default static data
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacksAndMessages(null);
        }
    }
}
