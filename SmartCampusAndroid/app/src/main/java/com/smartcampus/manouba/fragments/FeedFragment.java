package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.PostsAdapter;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.Constants;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedFragment extends Fragment implements PostsAdapter.OnComposeClickListener {

    private RecyclerView rvFeed;
    private SwipeRefreshLayout swipeRefresh;
    private PostsAdapter adapter;
    private List<JsonObject> posts = new ArrayList<>();

    // Real-time polling
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFeed = view.findViewById(R.id.rv_feed);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFeed.setItemAnimator(null); // prevents flicker on poll updates

        adapter = new PostsAdapter(posts, requireContext(), this);
        adapter.setLikeListener((postId, isCurrentlyLiked, position) -> handleLike(postId, isCurrentlyLiked, position));
        adapter.setCommentListener((postId, position) -> handleComment(postId, position));
        adapter.setRepostListener((postId, position) -> handleRepost(postId, position));
        rvFeed.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadFeed);
        loadFeed();
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
        loadFeedSilent(); // silent refresh immediately on resume
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                loadFeedSilent(); // silent refresh — no spinner
                pollHandler.postDelayed(this, Constants.POLL_FEED_MS);
            }
        };
        pollHandler.postDelayed(pollRunnable, Constants.POLL_FEED_MS);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    // ── Feed loading ─────────────────────────────────────────────────────────

    public void loadFeed() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        fetchFeed(true);
    }

    private void loadFeedSilent() {
        fetchFeed(false);
    }

    private void fetchFeed(boolean showSpinner) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        String token = SharedPrefManager.getInstance(ctx).getToken();
        RetrofitClient.getInstance(token).getApi().getFeed()
                .enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JsonObject>> call,
                                           @NonNull Response<List<JsonObject>> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (showSpinner && swipeRefresh != null) swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<JsonObject> newPosts = response.body();
                            // Only update if something changed (avoids scroll jump on silent refresh)
                            if (!postsEqual(posts, newPosts)) {
                                posts.clear();
                                posts.addAll(newPosts);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        if (showSpinner && swipeRefresh != null) swipeRefresh.setRefreshing(false);
                        if (showSpinner) {
                            Toast.makeText(getContext(),
                                    "Could not load feed. Check your connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /** Simple equality check — avoids full redraw when nothing changed. */
    private boolean postsEqual(List<JsonObject> a, List<JsonObject> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            int idA = a.get(i).has("id") ? a.get(i).get("id").getAsInt() : -1;
            int idB = b.get(i).has("id") ? b.get(i).get("id").getAsInt() : -1;
            int likesA = a.get(i).has("likes_count") ? a.get(i).get("likes_count").getAsInt() : 0;
            int likesB = b.get(i).has("likes_count") ? b.get(i).get("likes_count").getAsInt() : 0;
            if (idA != idB || likesA != likesB) return false;
        }
        return true;
    }

    // ── Post interactions ─────────────────────────────────────────────────────

    private void handleLike(int postId, boolean isCurrentlyLiked, int position) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        Callback<JsonObject> cb = new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                boolean nowLiked = !isCurrentlyLiked;
                int newCount = response.body().has("likes_count")
                        ? response.body().get("likes_count").getAsInt() : 0;
                if (position < posts.size()) {
                    posts.get(position).addProperty("is_liked_by_me", nowLiked);
                    posts.get(position).addProperty("likes_count", newCount);
                    adapter.notifyItemChanged(position);
                }
            }
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Like failed", Toast.LENGTH_SHORT).show();
            }
        };
        if (isCurrentlyLiked) {
            RetrofitClient.getInstance(token).getApi().unlikePost(postId).enqueue(cb);
        } else {
            RetrofitClient.getInstance(token).getApi().likePost(postId).enqueue(cb);
        }
    }

    private void handleComment(int postId, int position) {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Write a comment...");
        input.setPadding(48, 32, 48, 32);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Comment")
                .setView(input)
                .setPositiveButton("Post", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    String token = SharedPrefManager.getInstance(requireContext()).getToken();
                    JsonObject body = new JsonObject();
                    body.addProperty("content", text);
                    RetrofitClient.getInstance(token).getApi().addComment(postId, body)
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(@NonNull Call<JsonObject> call,
                                                       @NonNull Response<JsonObject> response) {
                                    if (!isAdded()) return;
                                    if (response.isSuccessful()) {
                                        if (position < posts.size()) {
                                            int cur = posts.get(position).has("comments_count")
                                                    ? posts.get(position).get("comments_count").getAsInt() : 0;
                                            posts.get(position).addProperty("comments_count", cur + 1);
                                            adapter.notifyItemChanged(position);
                                        }
                                        Toast.makeText(requireContext(), "Comment posted!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                                    if (isAdded()) Toast.makeText(requireContext(), "Failed to post comment", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleRepost(int postId, int position) {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Add your thoughts... (optional)");
        input.setPadding(48, 32, 48, 32);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Repost")
                .setView(input)
                .setPositiveButton("Repost", (d, w) -> {
                    String text = input.getText().toString().trim();
                    String token = SharedPrefManager.getInstance(requireContext()).getToken();
                    JsonObject body = new JsonObject();
                    body.addProperty("content", text);
                    RetrofitClient.getInstance(token).getApi().repost(postId, body)
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(@NonNull Call<JsonObject> call,
                                                       @NonNull Response<JsonObject> response) {
                                    if (!isAdded()) return;
                                    if (response.isSuccessful()) {
                                        Toast.makeText(requireContext(), "Reposted!", Toast.LENGTH_SHORT).show();
                                        loadFeed();
                                    }
                                }
                                @Override
                                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── OnComposeClickListener (from PostsAdapter header) ───────────────────

    @Override
    public void onComposeClick() {
        // Navigate to the Compose tab (index 1) in SocialHubFragment's ViewPager
        if (getParentFragment() instanceof SocialHubFragment) {
            View pagerView = getParentFragment().getView();
            if (pagerView != null) {
                ViewPager2 vp = pagerView.findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(1, true);
            }
        }
    }
}
