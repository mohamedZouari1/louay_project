package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.UserSearchAdapter;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchUsersFragment extends Fragment implements UserSearchAdapter.OnUserClickListener {

    private TextInputEditText etSearch;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private RecyclerView rvUsers;

    private UserSearchAdapter adapter;
    private final List<JsonObject> users = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch    = view.findViewById(R.id.et_search);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState  = view.findViewById(R.id.empty_state);
        rvUsers     = view.findViewById(R.id.rv_users);

        adapter = new UserSearchAdapter(requireContext(), users, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    users.clear();
                    adapter.notifyDataSetChanged();
                    rvUsers.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void searchUsers(String query) {
        progressBar.setVisibility(View.VISIBLE);
        rvUsers.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        String token = SharedPrefManager.getInstance(requireContext()).getToken();
        RetrofitClient.getInstance(token).getApi().searchUsers(query)
                .enqueue(new Callback<List<JsonObject>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<JsonObject>> call,
                                           @NonNull Response<List<JsonObject>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            users.clear();
                            users.addAll(response.body());
                            adapter.notifyDataSetChanged();

                            if (users.isEmpty()) {
                                rvUsers.setVisibility(View.GONE);
                                emptyState.setVisibility(View.VISIBLE);
                            } else {
                                rvUsers.setVisibility(View.VISIBLE);
                                emptyState.setVisibility(View.GONE);
                            }
                        } else {
                            showError(getString(R.string.error_network));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    @Override
    public void onUserClick(int userId, String userName) {
        Bundle args = new Bundle();
        args.putInt("userId", userId);
        args.putString("userName", userName);
        Navigation.findNavController(requireView())
                .navigate(R.id.profileDetailFragment, args);
    }

    private void showError(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
