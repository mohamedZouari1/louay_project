package com.smartcampus.manouba.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.Conversation;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private final List<Conversation> conversations;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Conversation conversation);
    }

    public ChatListAdapter(List<Conversation> conversations, OnChatClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        
        holder.tvName.setText(conversation.getName());
        holder.tvLastMessage.setText(conversation.getLastMessage());
        holder.tvTime.setText(conversation.getTime());

        // Set initials
        String name = conversation.getName();
        String initials = "";
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                initials += parts[0].charAt(0);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                initials += parts[1].charAt(0);
            }
        }
        if (initials.isEmpty()) initials = "?";
        holder.tvInitials.setText(initials.toUpperCase());

        // Online indicator
        holder.viewOnline.setVisibility(conversation.isOnline() ? View.VISIBLE : View.GONE);

        // Unread dot
        holder.viewUnread.setVisibility(conversation.getUnreadCount() > 0 ? View.VISIBLE : View.GONE);

        // If it's a group, change avatar color slightly
        if (conversation.isGroup()) {
            holder.viewAvatarBg.setBackgroundResource(R.drawable.shape_circle_accent);
        } else {
            holder.viewAvatarBg.setBackgroundResource(R.drawable.shape_circle_primary);
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMessage, tvTime, tvInitials;
        View viewOnline, viewUnread, viewAvatarBg;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMessage = itemView.findViewById(R.id.tv_chat_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
            tvInitials = itemView.findViewById(R.id.tv_chat_initials);
            viewOnline = itemView.findViewById(R.id.view_online_indicator);
            viewUnread = itemView.findViewById(R.id.view_unread_dot);
            viewAvatarBg = itemView.findViewById(R.id.view_avatar_bg);
        }
    }
}
