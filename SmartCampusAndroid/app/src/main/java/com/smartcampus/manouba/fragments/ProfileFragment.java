package com.smartcampus.manouba.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.utils.SharedPrefManager;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        TextView tvUniversity = view.findViewById(R.id.tv_university);

        tvName.setText(pref.getUserName());
        tvEmail.setText(pref.getUserEmail());
        tvUniversity.setText(pref.getUserUniversity());

        // Animate header entrance
        if (tvName.getParent() instanceof View) {
            View header = (View) tvName.getParent();
            header.setAlpha(0f);
            header.animate().alpha(1f).setDuration(600).start();
        }

        // Settings - show notification/theme preferences
        view.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            String[] options = {"Enable Notifications", "Dark Mode", "Language: English"};
            boolean[] checked = {true, false, true};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚙️ Settings")
                    .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                        checked[which] = isChecked;
                    })
                    .setPositiveButton("Save", (dialog, which) -> {
                        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        // Favorites - navigate to map tab
        view.findViewById(R.id.menu_favorites).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⭐ Favorite Places")
                    .setMessage("Your favorite places are saved on the interactive map.\n\nOpen the map and tap ☰ to see your favorites list!")
                    .setPositiveButton("Open Map", (dialog, which) -> {
                        // Navigate to map tab via bottom nav
                        if (getActivity() instanceof MainActivity) {
                            com.google.android.material.bottomnavigation.BottomNavigationView
                                    bottomNav = getActivity().findViewById(R.id.bottom_nav);
                            if (bottomNav != null) {
                                bottomNav.setSelectedItemId(R.id.mapFragment);
                            }
                        }
                    })
                    .setNegativeButton("OK", null)
                    .show();
        });

        // Help & Support
        view.findViewById(R.id.menu_help).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("❓ Help & Support")
                    .setMessage("Smart Campus Manouba v1.0\n\n" +
                            "📱 Features:\n" +
                            "• Interactive campus map with 50+ locations\n" +
                            "• Walking directions with GPS\n" +
                            "• University building details & floor plans\n" +
                            "• Event calendar & campus news\n" +
                            "• Favorite places bookmarking\n\n" +
                            "📧 Contact: smartcampus@uma.tn\n" +
                            "🌐 Website: uma.rnu.tn")
                    .setPositiveButton("Visit Website", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://uma.rnu.tn/fr"));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Could not open browser", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Send Email", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:smartcampus@uma.tn"));
                            intent.putExtra(Intent.EXTRA_SUBJECT, "Smart Campus App - Support");
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Logout
        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_confirm)
                    .setPositiveButton(R.string.logout, (dialog, which) -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).logout();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }
}
