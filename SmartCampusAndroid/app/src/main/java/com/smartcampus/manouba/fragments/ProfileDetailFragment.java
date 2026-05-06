package com.smartcampus.manouba.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileDetailFragment extends Fragment {

    private TextView tvAvatar, tvFullName, tvUniversity, tvBio;
    private TextView tvPostsCount, tvFollowersCount, tvFollowingCount;
    private Chip chipRole;
    private ProgressBar progressBar;
    private LinearLayout llPosts;

    private int userId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAvatar         = view.findViewById(R.id.tv_avatar);
        tvFullName       = view.findViewById(R.id.tv_full_name);
        tvUniversity     = view.findViewById(R.id.tv_university);
        tvBio            = view.findViewById(R.id.tv_bio);
        tvPostsCount     = view.findViewById(R.id.tv_posts_count);
        tvFollowersCount = view.findViewById(R.id.tv_followers_count);
        tvFollowingCount = view.findViewById(R.id.tv_following_count);
        chipRole         = view.findViewById(R.id.chip_role);
        progressBar      = view.findViewById(R.id.progress_bar);
        llPosts          = view.findViewById(R.id.ll_posts);

        View btnFollow = view.findViewById(R.id.btn_follow);
        View btnMessage = view.findViewById(R.id.btn_message);

        if (getArguments() != null) {
            userId = getArguments().getInt("userId", -1);
            String userName = getArguments().getString("userName", "Profile");
            if (getActivity() != null) getActivity().setTitle(userName);
            
            // Hide action buttons if viewing own profile
            int myId = SharedPrefManager.getInstance(requireContext()).getUserId();
            if (userId == myId || userId == -1) {
                if (btnFollow != null) btnFollow.setVisibility(View.GONE);
                if (btnMessage != null) btnMessage.setVisibility(View.GONE);
            } else {
                if (btnMessage != null) {
                    btnMessage.setOnClickListener(v -> {
                        Bundle args = new Bundle();
                        args.putString("chatId", String.valueOf(userId));
                        args.putString("chatName", userName);
                        androidx.navigation.Navigation.findNavController(v)
                                .navigate(R.id.chatMessagesFragment, args);
                    });
                }
                if (btnFollow != null) {
                    btnFollow.setOnClickListener(v -> toggleFollow());
                }
            }
        }

        if (userId != -1) loadProfile();
        else Toast.makeText(requireContext(), "Invalid user.", Toast.LENGTH_SHORT).show();
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getUserProfile(userId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call,
                                           @NonNull Response<JsonObject> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            JsonObject user = body.has("user") ? body.getAsJsonObject("user") : new JsonObject();
                            JsonArray  postsArr = body.has("posts") ? body.getAsJsonArray("posts") : new JsonArray();
                            
                            boolean isFollowing = user.has("is_following") && user.get("is_following").getAsBoolean();
                            updateFollowButton(isFollowing);
                            
                            populateProfile(user, postsArr);
                        } else {
                            showError("Could not load profile.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    private void populateProfile(JsonObject user, JsonArray postsArr) {
        String firstName = getString(user, "first_name");
        String lastName  = getString(user, "last_name");
        String fullName  = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) fullName = "User";

        // Initials avatar
        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty())  initials += lastName.charAt(0);
        tvAvatar.setText(initials.toUpperCase());

        // Avatar color
        String colorHex = getString(user, "avatar_color");
        try {
            Drawable bg = tvAvatar.getBackground().mutate();
            bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#1A237E" : colorHex),
                    PorterDuff.Mode.SRC_IN);
            tvAvatar.setBackground(bg);
        } catch (Exception ignored) {}

        tvFullName.setText(fullName);
        tvUniversity.setText(getString(user, "university"));

        String bio = getString(user, "bio");
        tvBio.setText(bio.isEmpty() ? "No bio yet." : bio);

        // Counts
        tvPostsCount.setText(String.valueOf(postsArr.size()));
        tvFollowersCount.setText(user.has("followers_count") ? user.get("followers_count").getAsString() : "0");
        tvFollowingCount.setText(user.has("following_count") ? user.get("following_count").getAsString() : "0");

        // Role chip
        String role = getString(user, "role");
        applyRoleChip(chipRole, role);

        // Posts list — inflate mini post rows directly into llPosts
        llPosts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < postsArr.size(); i++) {
            JsonObject post = postsArr.get(i).getAsJsonObject();
            View postView = inflater.inflate(R.layout.item_post_card, llPosts, false);

            // Avatar in post card
            TextView postAvatar = postView.findViewById(R.id.tv_avatar);
            postAvatar.setText(initials.toUpperCase());
            try {
                Drawable bg2 = postAvatar.getBackground().mutate();
                bg2.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#1A237E" : colorHex),
                        PorterDuff.Mode.SRC_IN);
                postAvatar.setBackground(bg2);
            } catch (Exception ignored) {}

            ((TextView) postView.findViewById(R.id.tv_author_name)).setText(fullName);
            ((TextView) postView.findViewById(R.id.tv_university)).setText(getString(user, "university"));
            ((TextView) postView.findViewById(R.id.tv_content)).setText(getString(post, "content"));
            ((TextView) postView.findViewById(R.id.tv_timestamp)).setText(formatRelativeTime(getString(post, "created_at")));

            int likes = post.has("likes_count") ? post.get("likes_count").getAsInt() : 0;
            ((TextView) postView.findViewById(R.id.tv_likes_count)).setText(String.valueOf(likes));

            applyRoleChip((Chip) postView.findViewById(R.id.chip_role), role);

            // Disable like button on profile detail view (read-only)
            postView.findViewById(R.id.btn_like).setEnabled(false);

            llPosts.addView(postView);
        }

        if (postsArr.size() == 0) {
            TextView empty = new TextView(requireContext());
            empty.setText("No posts yet.");
            empty.setTextColor(getResources().getColor(R.color.text_secondary, null));
            empty.setPadding(32, 24, 32, 24);
            llPosts.addView(empty);
        }
    }

    private void applyRoleChip(Chip chip, String role) {
        if (chip == null) return;
        switch (role) {
            case "org":
                chip.setText("Organization");
                chip.setChipBackgroundColorResource(R.color.cat_clubs);
                chip.setTextColor(Color.WHITE);
                break;
            case "admin":
                chip.setText("Administration");
                chip.setChipBackgroundColorResource(R.color.cat_administration);
                chip.setTextColor(Color.WHITE);
                break;
            default:
                chip.setText("Student");
                chip.setChipBackgroundColorResource(R.color.cat_services);
                chip.setTextColor(Color.WHITE);
                break;
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTime);
            long diffMs = System.currentTimeMillis() - date.getTime();
            long mins  = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMs);
            long days  = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs);
            if (mins  < 1)  return "Just now";
            if (mins  < 60) return mins  + "m ago";
            if (hours < 24) return hours + "h ago";
            return days + "d ago";
        } catch (Exception e) { return ""; }
    }

    private boolean isFollowing = false;

    private void updateFollowButton(boolean following) {
        this.isFollowing = following;
        View btnFollow = getView() != null ? getView().findViewById(R.id.btn_follow) : null;
        if (btnFollow instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) btnFollow;
            if (following) {
                btn.setText("Following");
                btn.setIcon(requireContext().getDrawable(R.drawable.ic_check));
                btn.setStrokeColorResource(R.color.divider);
            } else {
                btn.setText("Follow");
                btn.setIcon(requireContext().getDrawable(R.drawable.ic_plus));
                btn.setStrokeColorResource(R.color.primary);
            }
        }
    }

    private void toggleFollow() {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        Call<JsonObject> call;
        if (isFollowing) {
            call = RetrofitClient.getInstance(token).getApi().unfollowUser(userId);
        } else {
            call = RetrofitClient.getInstance(token).getApi().followUser(userId);
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isAdded() && response.isSuccessful()) {
                    updateFollowButton(!isFollowing);
                    // Refresh stats
                    loadProfile();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showError("Failed to update follow status.");
            }
        });
    }

    private void showError(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
