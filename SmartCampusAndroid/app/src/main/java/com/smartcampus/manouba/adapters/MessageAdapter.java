package com.smartcampus.manouba.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.ChatMessage;

import java.util.List;

import com.bumptech.glide.Glide;
import android.widget.ImageView;
import android.text.TextUtils;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;

    public MessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe() ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.tvMessage.setText(msg.getContent());
            h.tvMessage.setVisibility(TextUtils.isEmpty(msg.getContent()) ? View.GONE : View.VISIBLE);
            h.tvTime.setText(msg.getTime());
            
            if (!TextUtils.isEmpty(msg.getImageUrl())) {
                h.ivImage.setVisibility(View.VISIBLE);
                Glide.with(h.itemView.getContext()).load(msg.getImageUrl()).into(h.ivImage);
            } else {
                h.ivImage.setVisibility(View.GONE);
            }
        } else {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.tvMessage.setText(msg.getContent());
            h.tvMessage.setVisibility(TextUtils.isEmpty(msg.getContent()) ? View.GONE : View.VISIBLE);
            h.tvTime.setText(msg.getTime());

            if (!TextUtils.isEmpty(msg.getImageUrl())) {
                h.ivImage.setVisibility(View.VISIBLE);
                Glide.with(h.itemView.getContext()).load(msg.getImageUrl()).into(h.ivImage);
            } else {
                h.ivImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        SentViewHolder(View v) { 
            super(v); 
            tvMessage = v.findViewById(R.id.tv_message_sent); 
            tvTime = v.findViewById(R.id.tv_message_time_sent); 
            ivImage = v.findViewById(R.id.iv_message_image_sent);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        ReceivedViewHolder(View v) { 
            super(v); 
            tvMessage = v.findViewById(R.id.tv_message_received); 
            tvTime = v.findViewById(R.id.tv_message_time_received); 
            ivImage = v.findViewById(R.id.iv_message_image_received);
        }
    }
}
