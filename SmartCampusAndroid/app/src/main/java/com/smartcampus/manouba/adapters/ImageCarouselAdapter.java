package com.smartcampus.manouba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcampus.manouba.R;

public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ViewHolder> {

    private final int[] images;
    private final String[] captions;

    public ImageCarouselAdapter(int[] images, String[] captions) {
        this.images = images;
        this.captions = captions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivCarousel.setImageResource(images[position]);
        holder.tvCaption.setText(captions[position]);
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCarousel;
        TextView tvCaption;

        ViewHolder(View itemView) {
            super(itemView);
            ivCarousel = itemView.findViewById(R.id.iv_carousel);
            tvCaption = itemView.findViewById(R.id.tv_caption);
        }
    }
}
