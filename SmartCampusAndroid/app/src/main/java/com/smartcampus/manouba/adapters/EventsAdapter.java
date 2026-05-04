package com.smartcampus.manouba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcampus.manouba.R;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private final List<String[]> events; // [title, subtitle, description, date, imageName]

    public EventsAdapter(List<String[]> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] event = events.get(position);
        holder.tvEventTitle.setText(event[0]);
        holder.tvEventSubtitle.setText(event[1]);
        holder.tvEventDescription.setText(event[2]);
        holder.tvEventDate.setText(event[3]);

        // Set dynamic image from resources
        if (event.length > 4 && event[4] != null && !event[4].isEmpty()) {
            int resId = holder.itemView.getContext().getResources()
                    .getIdentifier(event[4], "drawable", holder.itemView.getContext().getPackageName());
            if (resId != 0) {
                holder.ivEventImage.setImageResource(resId);
                holder.ivEventImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.ivEventImage.setBackgroundColor(0);
            } else {
                holder.ivEventImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivEventImage.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.ivEventImage.setScaleType(ImageView.ScaleType.CENTER);
            holder.ivEventImage.setBackgroundColor(
                    holder.itemView.getResources().getColor(R.color.primary_light, null));
        }

        // Entrance animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(30f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(position * 150L)
                .start();
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventTitle, tvEventSubtitle, tvEventDescription, tvEventDate;

        ViewHolder(View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.iv_event_image);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventSubtitle = itemView.findViewById(R.id.tv_event_subtitle);
            tvEventDescription = itemView.findViewById(R.id.tv_event_description);
            tvEventDate = itemView.findViewById(R.id.tv_event_date);
        }
    }
}
