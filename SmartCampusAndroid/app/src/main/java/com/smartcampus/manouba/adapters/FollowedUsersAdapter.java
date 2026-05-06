package com.smartcampus.manouba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;

import java.util.List;

public class FollowedUsersAdapter extends RecyclerView.Adapter<FollowedUsersAdapter.ViewHolder> {

    private final List<JsonObject> users;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(JsonObject user);
    }

    public FollowedUsersAdapter(List<JsonObject> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_followed_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject user = users.get(position);
        String name = user.get("first_name").getAsString();
        String lastName = user.get("last_name").getAsString();
        
        holder.tvName.setText(name + " " + lastName);
        
        String initials = "";
        if (!name.isEmpty()) initials += name.charAt(0);
        if (!lastName.isEmpty()) initials += lastName.charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase());

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAvatar;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_followed_name);
            tvAvatar = itemView.findViewById(R.id.tv_followed_avatar);
        }
    }
}
