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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.fragments.SocialHubFragment;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_POST   = 1;

    public interface OnLikeClickListener {
        void onLikeClick(int postId, boolean currentlyLiked, int position);
    }

    private final Context context;
    private final List<JsonObject> posts;
    private final OnLikeClickListener likeListener;
    private final boolean showHeader;

    public PostsAdapter(Context context, List<JsonObject> posts, OnLikeClickListener likeListener, boolean showHeader) {
        this.context = context;
        this.posts = posts;
        this.likeListener = likeListener;
        this.showHeader = showHeader;
    }

    @Override
    public int getItemViewType(int position) {
        if (showHeader && position == 0) return TYPE_HEADER;
        return TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post_card, parent, false);
            return new PostViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeader((HeaderViewHolder) holder);
        } else {
            // If showHeader is true, the post data for index 1 is at list index 0
            int dataPos = showHeader ? position - 1 : position;
            bindPost((PostViewHolder) holder, dataPos);
        }
    }

    private void bindHeader(HeaderViewHolder holder) {
        String name = SharedPrefManager.getInstance(context).getUserName();
        String initials = "";
        String[] parts = name.trim().split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase());

        holder.btnStartPost.setOnClickListener(v -> {
            // Safer way to find the ViewPager2 across different context types
            try {
                View root = null;
                if (context instanceof android.app.Activity) {
                    root = ((android.app.Activity) context).getWindow().getDecorView();
                } else if (v.getRootView() != null) {
                    root = v.getRootView();
                }

                if (root != null) {
                    androidx.viewpager2.widget.ViewPager2 vp = root.findViewById(R.id.view_pager);
                    if (vp != null) vp.setCurrentItem(1, true);
                }
            } catch (Exception ignored) {}
        });
    }

    private void bindPost(PostViewHolder holder, int position) {
        JsonObject post  = posts.get(position);
        JsonObject author = post.has("author") && !post.get("author").isJsonNull()
                ? post.getAsJsonObject("author") : new JsonObject();

        String firstName = getString(author, "first_name");
        String lastName  = getString(author, "last_name");
        String fullName  = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) fullName = "Campus Member";
        holder.tvAuthorName.setText(fullName);

        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty())  initials += lastName.charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase(Locale.getDefault()));

        String colorHex = getString(author, "avatar_color");
        try {
            Drawable bg = holder.tvAvatar.getBackground().mutate();
            bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#0A66C2" : colorHex),
                    PorterDuff.Mode.SRC_IN);
            holder.tvAvatar.setBackground(bg);
        } catch (Exception ignored) {}

        String uni  = getString(author, "university");
        String role = getString(author, "role");
        if (role.isEmpty()) role = getString(post, "post_type");
        String roleLabel = getRoleLabel(role);
        holder.tvUniversity.setText(uni.isEmpty() ? roleLabel : uni + " · " + roleLabel);

        holder.tvTimestamp.setText(formatRelativeTime(getString(post, "created_at")));
        applyRoleChip(holder.chipRole, role);
        holder.tvContent.setText(getString(post, "content"));

        String imageUrl = getString(post, "image_url");
        if (!imageUrl.isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(imageUrl).centerCrop().into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        int likesCount = post.has("likes_count") ? post.get("likes_count").getAsInt() : 0;
        boolean isLiked = post.has("is_liked_by_me") && post.get("is_liked_by_me").getAsBoolean();
        holder.tvLikesCount.setText(likesCount > 0 ? String.valueOf(likesCount) : "");
        holder.llLikeSummary.setVisibility(likesCount > 0 ? View.VISIBLE : View.GONE);

        updateLikeButton(holder.ibLikeIcon, holder.tvLikeLabel, isLiked);

        holder.btnLikeRow.setOnClickListener(v -> {
            if (likeListener != null) {
                int postId = post.has("id") ? post.get("id").getAsInt() : -1;
                likeListener.onLikeClick(postId, isLiked, holder.getAdapterPosition());
            }
        });
    }

    public void toggleLike(int position, boolean liked, int newCount) {
        int dataPos = showHeader ? position - 1 : position;
        if (dataPos < 0 || dataPos >= posts.size()) return;
        JsonObject post = posts.get(dataPos);
        post.addProperty("is_liked_by_me", liked);
        post.addProperty("likes_count", newCount);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        int count = posts.size();
        if (showHeader) count++;
        return count;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private String getRoleLabel(String role) {
        switch (role) {
            case "org":   return "Organization";
            case "admin": return "Administration";
            default:      return "Student";
        }
    }

    private void applyRoleChip(Chip chip, String role) {
        switch (role) {
            case "org":
                chip.setText("Org");
                chip.setChipBackgroundColorResource(R.color.cat_clubs);
                chip.setTextColor(Color.WHITE);
                break;
            case "admin":
                chip.setText("Admin");
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

    private void updateLikeButton(ImageButton icon, TextView label, boolean liked) {
        if (liked) {
            icon.setImageResource(android.R.drawable.btn_star_big_on);
            icon.setColorFilter(Color.parseColor("#0A66C2"));
            label.setTextColor(Color.parseColor("#0A66C2"));
            label.setText(" Like");
        } else {
            icon.setImageResource(android.R.drawable.btn_star_big_off);
            icon.setColorFilter(Color.parseColor("#666666"));
            label.setTextColor(Color.parseColor("#666666"));
            label.setText(" Like");
        }
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
            Date date = sdf.parse(isoTime);
            if (date == null) return "";
            long diffMs = System.currentTimeMillis() - date.getTime();
            long mins  = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days  = TimeUnit.MILLISECONDS.toDays(diffMs);
            if (mins  < 1)  return "Just now";
            if (mins  < 60) return mins  + "m";
            if (hours < 24) return hours + "h";
            if (days  < 7)  return days  + "d";
            return days / 7 + "w";
        } catch (ParseException e) { return ""; }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar;
        MaterialButton btnStartPost;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_header_avatar);
            btnStartPost = itemView.findViewById(R.id.btn_start_post);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView   tvAvatar, tvAuthorName, tvUniversity, tvTimestamp, tvContent, tvLikesCount, tvLikeLabel;
        Chip       chipRole;
        ImageView  ivPostImage;
        ImageButton ibLikeIcon;
        LinearLayout btnLikeRow, llLikeSummary;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar       = itemView.findViewById(R.id.tv_avatar);
            tvAuthorName   = itemView.findViewById(R.id.tv_author_name);
            tvUniversity   = itemView.findViewById(R.id.tv_university);
            tvTimestamp    = itemView.findViewById(R.id.tv_timestamp);
            tvContent      = itemView.findViewById(R.id.tv_content);
            tvLikesCount   = itemView.findViewById(R.id.tv_likes_count);
            tvLikeLabel    = itemView.findViewById(R.id.tv_like_label);
            chipRole       = itemView.findViewById(R.id.chip_role);
            ivPostImage    = itemView.findViewById(R.id.iv_post_image);
            ibLikeIcon     = itemView.findViewById(R.id.ib_like_icon);
            btnLikeRow     = itemView.findViewById(R.id.btn_like);
            llLikeSummary  = itemView.findViewById(R.id.ll_like_summary);
        }
    }
}
