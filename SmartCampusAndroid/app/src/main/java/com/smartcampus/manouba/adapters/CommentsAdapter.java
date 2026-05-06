package com.smartcampus.manouba.adapters;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private final List<JsonObject> comments;

    public CommentsAdapter(List<JsonObject> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject comment = comments.get(position);
        JsonObject author = comment.has("author") ? comment.getAsJsonObject("author") : new JsonObject();

        String firstName = getString(author, "first_name");
        String lastName = getString(author, "last_name");
        holder.tvAuthor.setText(firstName + " " + lastName);
        holder.tvContent.setText(getString(comment, "content"));

        String avatarUrl = getString(author, "avatar");
        if (!avatarUrl.isEmpty()) {
            holder.tvAvatar.setVisibility(View.GONE);
            holder.ivAvatar.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(avatarUrl)
                .centerCrop()
                .into(holder.ivAvatar);
        } else {
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.ivAvatar.setVisibility(View.GONE);
            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.charAt(0);
            if (!lastName.isEmpty()) initials += lastName.charAt(0);
            holder.tvAvatar.setText(initials.toUpperCase());

            String colorHex = getString(author, "avatar_color");
            try {
                Drawable bg = holder.tvAvatar.getBackground().mutate();
                bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#0A66C2" : colorHex), PorterDuff.Mode.SRC_IN);
                holder.tvAvatar.setBackground(bg);
            } catch (Exception ignored) {}
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvAuthor, tvContent;
        android.widget.ImageView ivAvatar;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_comment_avatar);
            ivAvatar = itemView.findViewById(R.id.iv_comment_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}
