package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.ChatListAdapter;
import com.smartcampus.manouba.adapters.FollowedUsersAdapter;
import com.smartcampus.manouba.model.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView rvChats, rvFollowed;
    private SwipeRefreshLayout swipeRefresh;
    private ChatListAdapter adapter;
    private FollowedUsersAdapter followedAdapter;
    private final List<com.google.gson.JsonObject> followedUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChats = view.findViewById(R.id.rv_chats);
        rvChats.setLayoutManager(new LinearLayoutManager(requireContext()));

        rvFollowed = view.findViewById(R.id.rv_followed_users);
        rvFollowed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        
        followedAdapter = new FollowedUsersAdapter(followedUsers, user -> {
            // Start chat with this user
            Bundle args = new Bundle();
            args.putString("chatId", user.get("id").getAsString());
            args.putString("chatName", user.get("first_name").getAsString() + " " + user.get("last_name").getAsString());
            androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.chatMessagesFragment, args);
        });
        rvFollowed.setAdapter(followedAdapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_chats);
        swipeRefresh.setOnRefreshListener(() -> {
            loadConversations();
            loadFollowedUsers(view);
        });

        loadConversations();
        loadFollowedUsers(view);
    }

    private void loadConversations() {
        String token = com.smartcampus.manouba.utils.SharedPrefManager.getInstance(requireContext()).getToken();
        int myId = com.smartcampus.manouba.utils.SharedPrefManager.getInstance(requireContext()).getUserId();

        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().getConversations()
                .enqueue(new retrofit2.Callback<java.util.List<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.google.gson.JsonObject>> call, 
                                   retrofit2.Response<java.util.List<com.google.gson.JsonObject>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Conversation> convs = new ArrayList<>();
                    for (com.google.gson.JsonObject json : response.body()) {
                        try {
                            String id = json.get("id").getAsString();
                            
                            // Find the other participant's name
                            String name = "Conversation";
                            int otherUserId = -1;
                            com.google.gson.JsonArray parts = json.getAsJsonArray("participants");
                            for (int i = 0; i < parts.size(); i++) {
                                com.google.gson.JsonObject p = parts.get(i).getAsJsonObject();
                                if (p.get("id").getAsInt() != myId) {
                                    if (p.has("full_name") && !p.get("full_name").isJsonNull()) {
                                        name = p.get("full_name").getAsString();
                                    } else {
                                        name = p.get("first_name").getAsString() + " " + p.get("last_name").getAsString();
                                    }
                                    otherUserId = p.get("id").getAsInt();
                                    break;
                                }
                            }

                            String lastMsg = "No messages yet";
                            String time = "";
                            if (json.has("last_message") && !json.get("last_message").isJsonNull()) {
                                com.google.gson.JsonObject lm = json.getAsJsonObject("last_message");
                                lastMsg = lm.get("content").getAsString();
                                time = formatRelativeTime(lm.get("timestamp").getAsString());
                            }

                            Conversation c = new Conversation(String.valueOf(otherUserId), name, lastMsg, time, false, true);
                            convs.add(c);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    
                    adapter = new ChatListAdapter(convs, conversation -> {
                        Bundle args = new Bundle();
                        args.putString("chatId", conversation.getId());
                        args.putString("chatName", conversation.getName());
                        androidx.navigation.Navigation.findNavController(requireView())
                                .navigate(R.id.chatMessagesFragment, args);
                    });
                    rvChats.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.google.gson.JsonObject>> call, Throwable t) {}
        });
    }

    private void loadFollowedUsers(View view) {
        String token = com.smartcampus.manouba.utils.SharedPrefManager.getInstance(requireContext()).getToken();
        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().getFollowing()
                .enqueue(new retrofit2.Callback<List<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.google.gson.JsonObject>> call, retrofit2.Response<List<com.google.gson.JsonObject>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    followedUsers.clear();
                    followedUsers.addAll(response.body());
                    followedAdapter.notifyDataSetChanged();
                    
                    if (!followedUsers.isEmpty()) {
                        view.findViewById(R.id.followed_container).setVisibility(View.VISIBLE);
                    }
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<com.google.gson.JsonObject>> call, Throwable t) {
                if (isAdded()) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private String formatRelativeTime(String isoTime) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US);
            java.util.Date date = sdf.parse(isoTime);
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + "m";
            if (diff < 86400000) return (diff / 3600000) + "h";
            return new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date);
        } catch (Exception e) { return ""; }
    }
}
