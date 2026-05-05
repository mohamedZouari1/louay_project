package com.smartcampus.manouba.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;

import java.util.List;
import java.util.Locale;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(int userId, String userName);
    }

    private final Context context;
    private final List<JsonObject> users;
    private final OnUserClickListener clickListener;

    public UserSearchAdapter(Context context, List<JsonObject> users, OnUserClickListener clickListener) {
        this.context = context;
        this.users = users;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        JsonObject user = users.get(position);

        String firstName = getString(user, "first_name");
        String lastName  = getString(user, "last_name");
        String baseName  = (firstName + " " + lastName).trim();
        final String fullName = baseName.isEmpty() ? "Campus Member" : baseName;
        holder.tvName.setText(fullName);

        // Initials
        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty())  initials += lastName.charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase(Locale.getDefault()));

        // Avatar color
        String colorHex = getString(user, "avatar_color");
        try {
            Drawable bg = holder.tvAvatar.getBackground().mutate();
            bg.setColorFilter(Color.parseColor(colorHex.isEmpty() ? "#0A66C2" : colorHex),
                    PorterDuff.Mode.SRC_IN);
            holder.tvAvatar.setBackground(bg);
        } catch (Exception ignored) {}

        // University + role subtitle
        String uni    = getString(user, "university");
        String role   = getString(user, "role");
        String roleLbl = role.equals("org") ? "Organization" : role.equals("admin") ? "Administration" : "Student";
        holder.tvUniversity.setText(uni.isEmpty() ? roleLbl : uni + " · " + roleLbl);

        // Role chip
        switch (role) {
            case "org":
                holder.chipRole.setText("Org");
                holder.chipRole.setChipBackgroundColorResource(R.color.cat_clubs);
                holder.chipRole.setTextColor(Color.WHITE);
                break;
            case "admin":
                holder.chipRole.setText("Admin");
                holder.chipRole.setChipBackgroundColorResource(R.color.cat_administration);
                holder.chipRole.setTextColor(Color.WHITE);
                break;
            default:
                holder.chipRole.setText("Student");
                holder.chipRole.setChipBackgroundColorResource(R.color.cat_services);
                holder.chipRole.setTextColor(Color.WHITE);
                break;
        }

        // Both card tap and "View Profile" button navigate to profile
        int userId = user.has("id") ? user.get("id").getAsInt() : -1;
        View.OnClickListener openProfile = v -> {
            if (clickListener != null) clickListener.onUserClick(userId, fullName);
        };
        holder.itemView.setOnClickListener(openProfile);
        if (holder.btnViewProfile != null) holder.btnViewProfile.setOnClickListener(openProfile);
    }

    @Override
    public int getItemCount() { return users.size(); }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvUniversity;
        Chip chipRole;
        MaterialButton btnViewProfile;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar       = itemView.findViewById(R.id.tv_avatar);
            tvName         = itemView.findViewById(R.id.tv_name);
            tvUniversity   = itemView.findViewById(R.id.tv_university);
            chipRole       = itemView.findViewById(R.id.chip_role);
            btnViewProfile = itemView.findViewById(R.id.btn_view_profile);
        }
    }
}
