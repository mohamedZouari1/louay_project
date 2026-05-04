package com.smartcampus.manouba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcampus.manouba.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    private final String[] titles;
    private final String[] descriptions;
    private final int[] icons;

    public OnboardingAdapter(String[] titles, String[] descriptions, int[] icons) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.icons = icons;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtTitle.setText(titles[position]);
        holder.txtDescription.setText(descriptions[position]);
        holder.imgOnboarding.setImageResource(icons[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOnboarding;
        TextView txtTitle, txtDescription;

        ViewHolder(View itemView) {
            super(itemView);
            imgOnboarding = itemView.findViewById(R.id.img_onboarding);
            txtTitle = itemView.findViewById(R.id.txt_title);
            txtDescription = itemView.findViewById(R.id.txt_description);
        }
    }
}
