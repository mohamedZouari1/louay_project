package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.OnboardingAdapter;
import com.smartcampus.manouba.utils.SharedPrefManager;

public class OnboardingFragment extends Fragment {

    private ViewPager2 viewPager;
    private LinearLayout indicatorsLayout;
    private TextView btnNext, btnSkip;
    private View[] indicators;

    private final String[] titles = {
            "Welcome to Smart Campus",
            "Interactive Campus Map",
            "Stay Connected"
    };
    private final String[] descriptions = {
            "Your digital guide to University of Manouba",
            "Explore universities, restaurants, cafés, and more",
            "Access campus events, statistics, and resources"
    };
    private final int[] icons = {
            R.drawable.rectorat_image,
            R.drawable.isamm,
            R.drawable.greenspaces
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewpager_onboarding);
        indicatorsLayout = view.findViewById(R.id.layout_indicators);
        btnNext = view.findViewById(R.id.btn_next);
        btnSkip = view.findViewById(R.id.btn_skip);

        OnboardingAdapter adapter = new OnboardingAdapter(titles, descriptions, icons);
        viewPager.setAdapter(adapter);

        setupIndicators();
        updateIndicators(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
                if (position == titles.length - 1) {
                    btnNext.setText(R.string.get_started);
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnNext.setText(R.string.next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < titles.length - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void setupIndicators() {
        indicators = new View[titles.length];
        for (int i = 0; i < titles.length; i++) {
            indicators[i] = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            indicators[i].setLayoutParams(params);
            indicators[i].setBackgroundResource(R.drawable.shape_indicator);
            indicatorsLayout.addView(indicators[i]);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            if (i == position) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(24), dpToPx(8));
                params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
                indicators[i].setLayoutParams(params);
                indicators[i].setBackgroundResource(R.drawable.shape_indicator_active);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        dpToPx(8), dpToPx(8));
                params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
                indicators[i].setLayoutParams(params);
                indicators[i].setBackgroundResource(R.drawable.shape_indicator);
            }
        }
    }

    private void completeOnboarding() {
        SharedPrefManager.getInstance(requireContext()).setFirstLaunchDone();
        Navigation.findNavController(requireView())
                .navigate(R.id.action_onboarding_to_login);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
