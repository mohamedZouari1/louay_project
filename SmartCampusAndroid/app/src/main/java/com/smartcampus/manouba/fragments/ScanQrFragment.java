package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.smartcampus.manouba.R;

public class ScanQrFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_scan_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Laser Animation
        View laser = view.findViewById(R.id.scanner_laser);
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, 650); // Adjusted for frame size
        animation.setDuration(2000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        laser.startAnimation(animation);

        view.findViewById(R.id.btn_simulate_scan).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "QR Code Scanned: Campus Cafeteria - 5.500 TND", Toast.LENGTH_LONG).show();
            // Logic to deduct money would go here
            Navigation.findNavController(v).navigateUp();
        });
    }
}
