package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.smartcampus.manouba.R;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.switch_push).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Notification settings updated", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.switch_dark_mode).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Dark mode will be applied on next restart", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_change_password).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Opening privacy policy...", Toast.LENGTH_SHORT).show());
    }
}
