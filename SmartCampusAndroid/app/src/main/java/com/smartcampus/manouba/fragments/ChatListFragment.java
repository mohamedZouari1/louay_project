package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.smartcampus.manouba.utils.Constants;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatListFragment extends Fragment {

    private RecyclerView rvChats, rvFollowed;
    private SwipeRefreshLayout swipeRefresh;
    private ChatListAdapter adapter;
    private FollowedUsersAdapter followedAdapter;
    private final List<com.google.gson.JsonObject> followedUsers = new ArrayList<>();

    // Real-time polling
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

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
        rvChats.setItemAnimator(null);

        rvFollowed = view.findViewById(R.id.rv_followed_users);
        rvFollowed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        followedAdapter = new FollowedUsersAdapter(followedUsers, user -> {
            try {
                Bundle args = new Bundle();
                args.putString("chatId", user.get("id").getAsString());
                String firstName = user.has("first_name") ? user.get("first_name").getAsString() : "";
                String lastName  = user.has("last_name")  ? user.get("last_name").getAsString()  : "";
                args.putString("chatName", (firstName + " " + lastName).trim());
                androidx.navigation.Navigation.findNavController(requireView())
                        .navigate(R.id.chatMessagesFragment, args);
            } catch (Exception ignored) {}
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

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    // ── Polling ──────────────────────────────────────────────────────────────

    private void startPolling() {
        stopPolling();
        loadConversationsSilent(); // load immediately on resume
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                loadConversationsSilent();
                pollHandler.postDelayed(this, Constants.POLL_CHAT_MS * 3); // every 15s for list
            }
        };
        pollHandler.postDelayed(pollRunnable, Constants.POLL_CHAT_MS * 3);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    // ── Conversations ─────────────────────────────────────────────────────────

    private void loadConversations() {
        fetchConversations(true);
    }

    private void loadConversationsSilent() {
        fetchConversations(false);
    }

    private void fetchConversations(boolean showSpinner) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        int myId = SharedPrefManager.getInstance(requireContext()).getUserId();

        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().getConversations()
                .enqueue(new retrofit2.Callback<List<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.google.gson.JsonObject>> call,
                                   retrofit2.Response<List<com.google.gson.JsonObject>> response) {
                if (!isAdded()) return;
                if (swipeRefresh != null && showSpinner) swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<com.google.gson.JsonObject> rawList = new ArrayList<>(response.body());
                    // Sort rawList by last message timestamp (falling back to updated_at) descending
                    java.util.Collections.sort(rawList, (o1, o2) -> {
                        String time1 = "";
                        if (o1.has("last_message") && !o1.get("last_message").isJsonNull()) {
                            com.google.gson.JsonObject lm = o1.getAsJsonObject("last_message");
                            if (lm.has("timestamp") && !lm.get("timestamp").isJsonNull()) {
                                time1 = lm.get("timestamp").getAsString();
                            }
                        }
                        if (time1.isEmpty() && o1.has("updated_at") && !o1.get("updated_at").isJsonNull()) {
                            time1 = o1.get("updated_at").getAsString();
                        }

                        String time2 = "";
                        if (o2.has("last_message") && !o2.get("last_message").isJsonNull()) {
                            com.google.gson.JsonObject lm = o2.getAsJsonObject("last_message");
                            if (lm.has("timestamp") && !lm.get("timestamp").isJsonNull()) {
                                time2 = lm.get("timestamp").getAsString();
                            }
                        }
                        if (time2.isEmpty() && o2.has("updated_at") && !o2.get("updated_at").isJsonNull()) {
                            time2 = o2.get("updated_at").getAsString();
                        }

                        Date d1 = parseIsoDate(time1);
                        Date d2 = parseIsoDate(time2);
                        return d2.compareTo(d1); // Newest first
                    });

                    List<Conversation> convs = new ArrayList<>();
                    for (com.google.gson.JsonObject json : rawList) {
                        try {
                            com.google.gson.JsonArray parts = json.getAsJsonArray("participants");
                            String name = "Conversation";
                            int otherUserId = -1;

                            for (int i = 0; i < parts.size(); i++) {
                                com.google.gson.JsonObject p = parts.get(i).getAsJsonObject();
                                if (p.get("id").getAsInt() != myId) {
                                    if (p.has("full_name") && !p.get("full_name").isJsonNull()) {
                                        name = p.get("full_name").getAsString();
                                    } else {
                                        String fn = p.has("first_name") && !p.get("first_name").isJsonNull()
                                                ? p.get("first_name").getAsString() : "";
                                        String ln = p.has("last_name") && !p.get("last_name").isJsonNull()
                                                ? p.get("last_name").getAsString() : "";
                                        name = (fn + " " + ln).trim();
                                        if (name.isEmpty()) name = "User";
                                    }
                                    otherUserId = p.get("id").getAsInt();
                                    break;
                                }
                            }

                            String lastMsg = "No messages yet";
                            String time = "";

                            if (json.has("last_message") && !json.get("last_message").isJsonNull()) {
                                com.google.gson.JsonObject lm = json.getAsJsonObject("last_message");
                                // Safe content extraction (can be null or empty for file-only messages)
                                if (lm.has("content") && !lm.get("content").isJsonNull()) {
                                    String content = lm.get("content").getAsString().trim();
                                    lastMsg = content.isEmpty() ? "📎 Attachment" : content;
                                } else {
                                    lastMsg = "📎 Attachment";
                                }
                                if (lm.has("timestamp") && !lm.get("timestamp").isJsonNull()) {
                                    time = formatRelativeTime(lm.get("timestamp").getAsString());
                                }
                            }

                            // Unread count
                            int unreadCount = 0;
                            if (json.has("unread_count") && !json.get("unread_count").isJsonNull()) {
                                unreadCount = json.get("unread_count").getAsInt();
                            }

                            Conversation c = new Conversation(
                                    String.valueOf(otherUserId), name, lastMsg, time, false, true);
                            c.setUnreadCount(unreadCount);
                            convs.add(c);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final int finalMyId = myId;
                    adapter = new ChatListAdapter(convs, conversation -> {
                        try {
                            Bundle args = new Bundle();
                            args.putString("chatId", conversation.getId());
                            args.putString("chatName", conversation.getName());
                            androidx.navigation.Navigation.findNavController(requireView())
                                    .navigate(R.id.chatMessagesFragment, args);
                        } catch (Exception ignored) {}
                    });
                    rvChats.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<com.google.gson.JsonObject>> call, Throwable t) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadFollowedUsers(View view) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi().getFollowing()
                .enqueue(new retrofit2.Callback<List<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.google.gson.JsonObject>> call,
                                   retrofit2.Response<List<com.google.gson.JsonObject>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    followedUsers.clear();
                    followedUsers.addAll(response.body());
                    followedAdapter.notifyDataSetChanged();

                    View container = view.findViewById(R.id.followed_container);
                    if (container != null) {
                        container.setVisibility(followedUsers.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<com.google.gson.JsonObject>> call, Throwable t) {
                if (isAdded() && swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private Date parseIsoDate(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return new Date(0);
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(isoTime);
                if (date != null) return date;
            } catch (ParseException ignored) {}
        }
        return new Date(0);
    }

    // ── Timestamp formatting ──────────────────────────────────────────────────

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(isoTime);
                if (date == null) continue;
                long diff = System.currentTimeMillis() - date.getTime();
                if (diff < 60_000)        return "Just now";
                if (diff < 3_600_000)     return (diff / 60_000) + "m";
                if (diff < 86_400_000)    return (diff / 3_600_000) + "h";
                return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
            } catch (ParseException ignored) {}
        }
        return "";
    }
}
