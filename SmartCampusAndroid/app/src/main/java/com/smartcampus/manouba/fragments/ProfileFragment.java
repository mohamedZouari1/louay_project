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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.Activity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.network.RetrofitClient;
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

        // Load real stats and bio
        loadFullProfile(view);

        // Animate header entrance
        View header = view.findViewById(R.id.profile_header_card);
        if (header != null) {
            header.setAlpha(0f);
            header.animate().alpha(1f).setDuration(600).start();
        }

        // Edit Profile
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> showEditProfileDialog());

        // Settings - navigate to full screen settings
        view.findViewById(R.id.btn_profile_settings).setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v).navigate(R.id.settingsFragment);
        });

        // The old menu items still work for secondary actions
        view.findViewById(R.id.menu_settings).setOnClickListener(v -> view.findViewById(R.id.btn_profile_settings).performClick());
        
        // Favorites - show custom bottom sheet
        view.findViewById(R.id.menu_favorites).setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                    new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
            View sheetView = getLayoutInflater().inflate(R.layout.dialog_favorites_info, null);
            dialog.setContentView(sheetView);
            
            sheetView.findViewById(R.id.btn_open_map).setOnClickListener(btn -> {
                androidx.navigation.Navigation.findNavController(view).navigate(R.id.mapFragment);
                dialog.dismiss();
            });
            dialog.show();
        });

        // Help & Support - show custom bottom sheet
        view.findViewById(R.id.menu_help).setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                    new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
            View sheetView = getLayoutInflater().inflate(R.layout.dialog_help_support, null);
            dialog.setContentView(sheetView);
            
            sheetView.findViewById(R.id.btn_visit_website).setOnClickListener(btn -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://uma.rnu.tn/fr"));
                    startActivity(intent);
                } catch (Exception ignored) {}
                dialog.dismiss();
            });
            dialog.show();
        });

        // Change Avatar
        view.findViewById(R.id.btn_change_avatar).setOnClickListener(v -> openImagePicker());

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

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    uploadProfileImage(selectedImageUri);
                }
            });

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri uri) {
        try {
            java.io.File file = uriToFile(uri);
            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), file);
            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("avatar", file.getName(), reqFile);
            
            String token = SharedPrefManager.getInstance(requireContext()).getToken();
            RetrofitClient.getInstance(token).getApi().updateProfileImage(body).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                    if (isAdded() && response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();
                        loadFullProfile(getView());
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error preparing image", Toast.LENGTH_SHORT).show();
        }
    }

    private java.io.File uriToFile(Uri uri) throws Exception {
        java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        java.io.File tempFile = java.io.File.createTempFile("avatar_", ".jpg", requireContext().getCacheDir());
        tempFile.deleteOnExit();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        }
        inputStream.close();
        return tempFile;
    }

    private void showEditProfileDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(sheetView);

        com.google.android.material.textfield.TextInputEditText etFirst = sheetView.findViewById(R.id.et_first_name);
        com.google.android.material.textfield.TextInputEditText etLast = sheetView.findViewById(R.id.et_last_name);
        com.google.android.material.textfield.TextInputEditText etUni = sheetView.findViewById(R.id.et_university);
        com.google.android.material.textfield.TextInputEditText etBio = sheetView.findViewById(R.id.et_bio);
        View btnSave = sheetView.findViewById(R.id.btn_save_profile);

        // Pre-fill with current data
        SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
        String fullName = pref.getUserName();
        String[] parts = fullName.split(" ", 2);
        etFirst.setText(parts[0]);
        if (parts.length > 1) etLast.setText(parts[1]);
        etUni.setText(pref.getUserUniversity());
        
        TextView tvBio = getView().findViewById(R.id.tv_bio);
        if (tvBio != null) {
            String currentBio = tvBio.getText().toString();
            if (!currentBio.startsWith("No bio")) etBio.setText(currentBio);
        }

        String selectedColor[] = {"#1A237E"}; // Default blue
        
        sheetView.findViewById(R.id.color_blue).setOnClickListener(v -> selectedColor[0] = "#1A237E");
        sheetView.findViewById(R.id.color_amber).setOnClickListener(v -> selectedColor[0] = "#FFC107");
        sheetView.findViewById(R.id.color_green).setOnClickListener(v -> selectedColor[0] = "#4CAF50");
        sheetView.findViewById(R.id.color_purple).setOnClickListener(v -> selectedColor[0] = "#673AB7");
        sheetView.findViewById(R.id.color_red).setOnClickListener(v -> selectedColor[0] = "#F44336");

        btnSave.setOnClickListener(v -> {
            String fName = etFirst.getText().toString().trim();
            String lName = etLast.getText().toString().trim();
            String uni = etUni.getText().toString().trim();
            String bio = etBio.getText().toString().trim();

            if (fName.isEmpty()) {
                etFirst.setError("First name required");
                return;
            }

            com.google.gson.JsonObject body = new com.google.gson.JsonObject();
            body.addProperty("first_name", fName);
            body.addProperty("last_name", lName);
            body.addProperty("university", uni);
            body.addProperty("bio", bio);
            body.addProperty("avatar_color", selectedColor[0]);

            String token = pref.getToken();
            RetrofitClient.getInstance(token).getApi().updateProfile(body).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                    if (isAdded() && response.isSuccessful()) {
                        // Update local prefs
                        pref.saveUserName(fName + " " + lName);
                        pref.saveUserUniversity(uni);
                        
                        // Refresh UI
                        loadFullProfile(getView());
                        
                        // Also update top header text directly for immediate feedback
                        TextView tvName = getView().findViewById(R.id.tv_name);
                        TextView tvUniversity = getView().findViewById(R.id.tv_university);
                        if (tvName != null) tvName.setText(fName + " " + lName);
                        if (tvUniversity != null) tvUniversity.setText(uni);

                        Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void loadFullProfile(View view) {
        int myId = SharedPrefManager.getInstance(requireContext()).getUserId();
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        
        RetrofitClient.getInstance(token).getApi().getUserProfile(myId).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    com.google.gson.JsonObject body = response.body();
                    com.google.gson.JsonObject user = body.has("user") ? body.getAsJsonObject("user") : new com.google.gson.JsonObject();
                    
                    TextView tvAvatar = view.findViewById(R.id.tv_profile_avatar);
                    com.google.android.material.imageview.ShapeableImageView ivAvatar = view.findViewById(R.id.iv_profile_avatar);
                    TextView tvBio = view.findViewById(R.id.tv_bio);
                    TextView tvPosts = view.findViewById(R.id.tv_posts_count);
                    TextView tvFollowers = view.findViewById(R.id.tv_followers_count);
                    TextView tvFollowing = view.findViewById(R.id.tv_following_count);

                    // Avatar logic
                    String avatarUrl = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : null;
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (ivAvatar != null) {
                            ivAvatar.setVisibility(View.VISIBLE);
                            com.bumptech.glide.Glide.with(requireContext())
                                .load(avatarUrl)
                                .centerCrop()
                                .into(ivAvatar);
                        }
                        if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
                    } else {
                        if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
                        if (tvAvatar != null) {
                            tvAvatar.setVisibility(View.VISIBLE);
                            String name = SharedPrefManager.getInstance(requireContext()).getUserName();
                            String initials = "";
                            String[] parts = name.trim().split(" ");
                            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
                            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
                            tvAvatar.setText(initials.toUpperCase());
                            
                            // Apply custom color
                            String colorStr = user.has("avatar_color") && !user.get("avatar_color").isJsonNull() 
                                    ? user.get("avatar_color").getAsString() : "#1A237E";
                            try {
                                tvAvatar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorStr)));
                            } catch (Exception ignored) {}
                        }
                    }

                    if (tvBio != null) {
                        String bio = user.has("bio") ? user.get("bio").getAsString() : "";
                        tvBio.setText(bio.isEmpty() ? "No bio yet. Tap to add one!" : bio);
                    }
                    if (tvPosts != null && body.has("posts")) {
                        tvPosts.setText(String.valueOf(body.getAsJsonArray("posts").size()));
                    }
                    if (tvFollowers != null) {
                        tvFollowers.setText(user.has("followers_count") ? user.get("followers_count").getAsString() : "0");
                    }
                    if (tvFollowing != null) {
                        tvFollowing.setText(user.has("following_count") ? user.get("following_count").getAsString() : "0");
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {}
        });
    }
}
