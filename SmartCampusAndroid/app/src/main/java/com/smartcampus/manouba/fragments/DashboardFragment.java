package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.EventsAdapter;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.adapters.ImageCarouselAdapter;
import com.smartcampus.manouba.adapters.StatsAdapter;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardFragment extends Fragment {

    private ViewPager2 viewPagerCarousel;
    private LinearLayout carouselIndicators;
    private RecyclerView rvStats, rvEvents;
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
        rvEvents = view.findViewById(R.id.rv_events);

        setupCarousel();
        setupStats();
        setupEvents();
        setupQuickActions(view);
        setupProfileButton(view);

        // Try to load from API, but fallback to static data if it fails
        try {
            loadDataFromApi();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupProfileButton(View view) {
        View cardProfile = view.findViewById(R.id.card_student_profile);
        TextView tvAvatar = view.findViewById(R.id.tv_dashboard_avatar);
        com.google.android.material.imageview.ShapeableImageView ivAvatar = view.findViewById(R.id.iv_dashboard_avatar);
        TextView tvGreeting = view.findViewById(R.id.tv_dashboard_greeting);
        TextView tvName = view.findViewById(R.id.tv_dashboard_name);

        if (cardProfile != null) {
            String fullName = SharedPrefManager.getInstance(requireContext()).getUserName();
            String firstName = "Student";
            
            if (fullName != null && !fullName.isEmpty()) {
                String[] parts = fullName.trim().split(" ");
                firstName = parts[0];
                if (tvName != null) tvName.setText(fullName);
                
                // Set initials for avatar as fallback
                String initials = "" + parts[0].charAt(0);
                if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
                if (tvAvatar != null) tvAvatar.setText(initials.toUpperCase());
            }

            if (tvGreeting != null) {
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour < 12) greeting = "Good morning, ";
                else if (hour < 18) greeting = "Good afternoon, ";
                else greeting = "Good evening, ";
                
                tvGreeting.setText(greeting + firstName + "!");
            }

            cardProfile.setOnClickListener(v -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.profileFragment);
            });

            loadMyProfile(tvAvatar, ivAvatar);
        }
    }

    private void loadMyProfile(TextView tvAvatar, com.google.android.material.imageview.ShapeableImageView ivAvatar) {
        int myId = SharedPrefManager.getInstance(requireContext()).getUserId();
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        if (myId == -1 || token == null) return;

        RetrofitClient.getInstance(token).getApi().getUserProfile(myId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    JsonObject user = body.has("user") ? body.getAsJsonObject("user") : null;
                    if (user != null && user.has("avatar") && !user.get("avatar").isJsonNull()) {
                        String avatarUrl = user.get("avatar").getAsString();
                        if (!avatarUrl.isEmpty()) {
                            tvAvatar.setVisibility(View.GONE);
                            ivAvatar.setVisibility(View.VISIBLE);
                            com.bumptech.glide.Glide.with(requireContext())
                                    .load(avatarUrl)
                                    .centerCrop()
                                    .into(ivAvatar);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void setupCarousel() {
        int[] images = {
                R.drawable.uma,
                R.drawable.image2,
                R.drawable.image44,
                R.drawable.image33,
                R.drawable.greenspaces,
                R.drawable.career_fair_3_0,
                R.drawable.tunihack_11_0,
                R.drawable.robocup_ensi_8
        };
        String[] captions = {
                "Main Campus Entrance", "Modern Learning Facilities",
                "Student Life", "Innovation & Technology", "Campus Green Spaces",
                "Career Fair 2025", "TuniHack 11.0", "RoboCup ENSI 8"
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

    private void setupEvents() {
        List<Event> events = new ArrayList<>();
        
        events.add(createEvent("12th Edition of UMA Symposium", "Nature/Culture",
                "Annual symposium exploring the intersection of nature and culture.", "Nov 12-14, 2025", "symposium"));
        events.add(createEvent("UMA Culture Day 25", "Carthage El Hadatha",
                "Celebrate the rich cultural heritage of the University of Manouba community.", "Dec 10-11, 2025", "cultureday"));
        events.add(createEvent("Hackathon Green UMA", "CIFIPP Lac 2",
                "Innovation hackathon focused on sustainable technology solutions.", "Jan 31 - Feb 1, 2026", "hackaton_uma"));
        events.add(createEvent("Manouba Networking Day", "Campus universitaire",
                "Annual networking event connecting students with industry professionals.", "Apr 30, 2025", "networkingday"));

        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rvEvents.setAdapter(new EventsAdapter(events));
    }

    private Event createEvent(String title, String subtitle, String desc, String date, String img) {
        // Since Event is a model with private fields and no constructor (likely for GSON)
        // We'll rely on our modified EventsAdapter to handle it, or add a constructor to Event.java
        // Actually, let's see Event.java again to see if I can add a constructor or if I should use reflection/GSON.
        // Better yet, I'll add a simple constructor to Event.java for local testing/static data.
        return new Event(title, subtitle, desc, date, img, null);
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
        String[] issueTypes = {"facilities", "accessibility", "safety", "technology", "other"};
        String[] issueLabels = {"Facilities & Infrastructure", "Accessibility", "Safety & Security", "Technology & IT", "Other"};

        final int[] selectedType = {0};

        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Describe the issue...");
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Report an Issue")
                .setSingleChoiceItems(issueLabels, 0, (d, which) -> {
                    selectedType[0] = which;
                })
                .setView(input)
                .setPositiveButton("Submit", (d, which) -> {
                    String desc = input.getText().toString().trim();
                    if (desc.isEmpty()) {
                        Toast.makeText(requireContext(), "Please provide a description", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitReport(issueTypes[selectedType[0]], desc);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReport(String type, String desc) {
        JsonObject body = new JsonObject();
        body.addProperty("issue_type", type);
        body.addProperty("description", desc);
        body.addProperty("location", "Campus Main");

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().submitReport(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isAdded() && response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Report submitted successfully! Administration will review it.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Failed to submit report. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
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
                public void onFailure(Call<List<JsonObject>> call, Throwable t) {}
            });

            RetrofitClient.getInstance(token).getApi().getEvents().enqueue(new Callback<List<Event>>() {
                @Override
                public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        if (rvEvents != null) rvEvents.setAdapter(new EventsAdapter(response.body()));
                    }
                }

                @Override
                public void onFailure(Call<List<Event>> call, Throwable t) {}
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
