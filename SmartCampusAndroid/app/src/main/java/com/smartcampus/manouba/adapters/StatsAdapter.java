package com.smartcampus.manouba.adapters;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcampus.manouba.R;

import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {

    private final List<String[]> stats; // [icon_name, value, label]

    public StatsAdapter(List<String[]> stats) {
        this.stats = stats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stat_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] stat = stats.get(position);
        String iconName = stat[0];
        String value = stat[1];
        String label = stat[2];

        // Set icon based on name
        int iconRes = getIconForName(iconName);
        holder.ivStatIcon.setImageResource(iconRes);
        holder.tvStatLabel.setText(label);

        // Animate count-up for numeric values
        holder.tvStatValue.setText(value);
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(position * 100L)
                .start();
    }

    private int getIconForName(String name) {
        switch (name) {
            case "people": return R.drawable.ic_stat_students;
            case "school": return R.drawable.ic_stat_teachers;
            case "book": return R.drawable.ic_stat_programs;
            case "business": return R.drawable.ic_stat_institutions;
            case "globe": return R.drawable.ic_stat_international;
            case "flask": return R.drawable.ic_stat_research;
            default: return R.drawable.ic_stat_teachers;
        }
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatIcon;
        TextView tvStatValue, tvStatLabel;

        ViewHolder(View itemView) {
            super(itemView);
            ivStatIcon = itemView.findViewById(R.id.iv_stat_icon);
            tvStatValue = itemView.findViewById(R.id.tv_stat_value);
            tvStatLabel = itemView.findViewById(R.id.tv_stat_label);
        }
    }
}
