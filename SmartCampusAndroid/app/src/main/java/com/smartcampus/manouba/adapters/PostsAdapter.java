package com.smartcampus.manouba.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    public interface OnLikeClickListener {
        void onLikeClick(int postId, boolean currentlyLiked, int position);
    }

    private final Context context;
    private final List<JsonObject> posts;
    private final OnLikeClickListener likeListener;

    public PostsAdapter(Context context, List<JsonObject> posts, OnLikeClickListener likeListener) {
        this.context = context;
        this.posts = posts;
        this.likeListener = likeListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        JsonObject post = posts.get(position);
        JsonObject author = post.has("author") && !post.get("author").isJsonNull()
                ? post.getAsJsonObject("author") : new JsonObject();

        // --- Author name & initials ---
        String firstName = getString(author, "first_name");
        String lastName = getString(author, "last_name");
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) fullName = "Unknown";
        holder.tvAuthorName.setText(fullName);

        // Avatar initials
        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty()) initials += lastName.charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase());

        // Avatar color
        String colorHex = getString(author, "avatar_color");
        try {
            Drawable bg = holder.tvAvatar.getBackground().mutate();
            bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#1A237E" : colorHex),
                    PorterDuff.Mode.SRC_IN);
            holder.tvAvatar.setBackground(bg);
        } catch (Exception ignored) {}

        // University
        holder.tvUniversity.setText(getString(author, "university"));

        // Timestamp
        holder.tvTimestamp.setText(formatRelativeTime(getString(post, "created_at")));

        // Role chip
        String role = getString(author, "role");
        if (role.isEmpty()) role = getString(post, "post_type");
        applyRoleChip(holder.chipRole, role);

        // Content
        holder.tvContent.setText(getString(post, "content"));

        // Image
        String imageUrl = getString(post, "image_url");
        if (!imageUrl.isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Likes
        int likesCount = post.has("likes_count") ? post.get("likes_count").getAsInt() : 0;
        boolean isLiked = post.has("is_liked_by_me") && post.get("is_liked_by_me").getAsBoolean();
        holder.tvLikesCount.setText(String.valueOf(likesCount));
        updateLikeButton(holder.btnLike, isLiked);

        holder.btnLike.setOnClickListener(v -> {
            if (likeListener != null) {
                int postId = post.has("id") ? post.get("id").getAsInt() : -1;
                likeListener.onLikeClick(postId, isLiked, holder.getAdapterPosition());
            }
        });
    }

    /** Toggle the visual liked/unliked state on a post at a given position. */
    public void toggleLike(int position, boolean liked, int newCount) {
        if (position < 0 || position >= posts.size()) return;
        JsonObject post = posts.get(position);
        post.addProperty("is_liked_by_me", liked);
        post.addProperty("likes_count", newCount);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private void applyRoleChip(Chip chip, String role) {
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

    private void updateLikeButton(ImageButton btn, boolean liked) {
        if (liked) {
            btn.setImageResource(android.R.drawable.btn_star_big_on);
            btn.setColorFilter(ContextCompat.getColor(context, R.color.error));
        } else {
            btn.setImageResource(android.R.drawable.btn_star_big_off);
            btn.setColorFilter(ContextCompat.getColor(context, R.color.gray));
        }
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            // ISO 8601 from Django: "2025-05-03T14:22:00.000000Z"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
            Date date = sdf.parse(isoTime);
            long diffMs = System.currentTimeMillis() - date.getTime();
            long mins = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            if (mins < 1) return "Just now";
            if (mins < 60) return mins + "m ago";
            if (hours < 24) return hours + "h ago";
            return days + "d ago";
        } catch (ParseException e) {
            return "";
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvAuthorName, tvUniversity, tvTimestamp, tvContent, tvLikesCount;
        Chip chipRole;
        ImageView ivPostImage;
        ImageButton btnLike;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvAuthorName = itemView.findViewById(R.id.tv_author_name);
            tvUniversity = itemView.findViewById(R.id.tv_university);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvLikesCount = itemView.findViewById(R.id.tv_likes_count);
            chipRole = itemView.findViewById(R.id.chip_role);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            btnLike = itemView.findViewById(R.id.btn_like);
        }
    }
}
