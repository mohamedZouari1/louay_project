package com.smartcampus.manouba.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {

    private final List<JsonObject> suggestions;
    private final Context context;

    public SuggestionsAdapter(Context context, List<JsonObject> suggestions) {
        this.context = context;
        this.suggestions = suggestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject user = suggestions.get(position);
        int userId = user.get("id").getAsInt();
        
        String firstName = user.has("first_name") && !user.get("first_name").isJsonNull() ? user.get("first_name").getAsString() : "";
        String lastName = user.has("last_name") && !user.get("last_name").isJsonNull() ? user.get("last_name").getAsString() : "";
        String rawName = (firstName + " " + lastName).trim();
        final String name = rawName.isEmpty() ? (user.has("username") && !user.get("username").isJsonNull() ? user.get("username").getAsString() : "Campus Member") : rawName;
        
        String uni = user.has("university") && !user.get("university").isJsonNull() ? user.get("university").getAsString() : "Student";
        String colorStr = user.has("avatar_color") && !user.get("avatar_color").isJsonNull() ? user.get("avatar_color").getAsString() : "#1A237E";

        holder.tvName.setText(name);
        holder.tvUni.setText(uni);
        
        // Initials
        String initials = "";
        String[] parts = name.trim().split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
        if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].charAt(0);
        holder.tvAvatar.setText(initials.toUpperCase());
        holder.tvAvatar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorStr)));

        boolean isFollowing = user.has("is_following") && !user.get("is_following").isJsonNull() && user.get("is_following").getAsBoolean();
        if (isFollowing) {
            holder.btnFollow.setText("Following");
            holder.btnFollow.setEnabled(false);
            holder.btnFollow.setAlpha(0.6f);
        } else {
            holder.btnFollow.setText("Follow");
            holder.btnFollow.setEnabled(true);
            holder.btnFollow.setAlpha(1.0f);
            holder.btnFollow.setOnClickListener(v -> {
                String token = SharedPrefManager.getInstance(context).getToken();
                RetrofitClient.getInstance(token).getApi().followUser(userId).enqueue(new retrofit2.Callback<JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            holder.btnFollow.setText("Following");
                            holder.btnFollow.setEnabled(false);
                            holder.btnFollow.setAlpha(0.6f);
                            Toast.makeText(context, "Following " + name, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {}
                });
            });
        }
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUni, tvAvatar;
        MaterialButton btnFollow;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_suggest_name);
            tvUni = itemView.findViewById(R.id.tv_suggest_uni);
            tvAvatar = itemView.findViewById(R.id.tv_suggest_avatar);
            btnFollow = itemView.findViewById(R.id.btn_suggest_follow);
        }
    }
}
