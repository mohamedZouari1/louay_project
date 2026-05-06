package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.PostsAdapter;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedFragment extends Fragment implements PostsAdapter.OnLikeClickListener {

    private RecyclerView rvFeed;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;

    private PostsAdapter adapter;
    private final List<JsonObject> posts = new ArrayList<>();
    private final List<JsonObject> suggestions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFeed       = view.findViewById(R.id.rv_feed);
        progressBar  = view.findViewById(R.id.progress_bar);
        emptyState   = view.findViewById(R.id.empty_state);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        // We now pass 'true' to show the LinkedIn header manually inside the adapter
        // We now pass suggestions list to the adapter
        adapter = new PostsAdapter(requireContext(), posts, suggestions, this, true);

        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFeed.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefresh.setOnRefreshListener(this::loadFeed);

        loadFeed();
        loadSuggestions();
        loadMyProfile();
    }

    private void loadMyProfile() {
        int myId = SharedPrefManager.getInstance(requireContext()).getUserId();
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getUserProfile(myId)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        JsonObject user = response.body().has("user") ? response.body().getAsJsonObject("user") : new JsonObject();
                        String avatarUrl = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : null;
                        if (avatarUrl != null) {
                            adapter.setMyAvatarUrl(avatarUrl);
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
            });
    }

    private void loadFeed() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getFeed()
                .enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JsonObject>> call,
                                           @NonNull Response<List<JsonObject>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            posts.clear();
                            posts.addAll(response.body());
                            adapter.notifyDataSetChanged();
                            emptyState.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            showError("Could not load feed (code " + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    @Override
    public void onLikeClick(int postId, boolean currentlyLiked, int position) {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        if (!currentlyLiked) {
            RetrofitClient.getInstance(token).getApi().likePost(postId)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call,
                                               @NonNull Response<JsonObject> response) {
                            if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                            int newCount = response.body().get("likes_count").getAsInt();
                            adapter.toggleLike(position, true, newCount);
                        }
                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
                    });
        } else {
            RetrofitClient.getInstance(token).getApi().unlikePost(postId)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call,
                                               @NonNull Response<JsonObject> response) {
                            if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                            int newCount = response.body().get("likes_count").getAsInt();
                            adapter.toggleLike(position, false, newCount);
                        }
                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {}
                    });
        }
    }

    private void loadSuggestions() {
        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().getSuggestions()
                .enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null) {
                            suggestions.clear();
                            suggestions.addAll(response.body());
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {}
                });
    }

    public void refreshFeed() { 
        loadFeed(); 
        loadSuggestions();
    }

    private void showError(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
