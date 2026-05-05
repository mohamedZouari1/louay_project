package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.EventsAdapter;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventsAdapter eventsAdapter;
    private AutoCompleteTextView spinner;
    private List<Event> allEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rv_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        spinner = view.findViewById(R.id.spinner_filter);
        setupSpinner();

        fetchEvents();

        spinner.setOnItemClickListener((parent, view1, position, id) -> {
            filterEvents(parent.getItemAtPosition(position).toString());
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.filter_options, android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setText("All", false);
    }

    private void fetchEvents() {
        // Show a loading toast or you could add a ProgressBar to your layout
        Toast.makeText(getContext(), "Loading events...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getInstance().getApi().getEvents().enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                if (!isAdded()) return;

                Log.d("EventsFragment", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    allEvents = response.body();
                    Log.d("EventsFragment", "Events received: " + allEvents.size());

                    if (allEvents.isEmpty()) {
                        Log.d("EventsFragment", "No events found from API");
                        Toast.makeText(getContext(), "No events available at the moment", Toast.LENGTH_LONG).show();
                    }

                    eventsAdapter = new EventsAdapter(new ArrayList<>(allEvents));
                    recyclerView.setAdapter(eventsAdapter);
                } else {
                    String errorMsg = "Failed to load events (Error " + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            Log.e("EventsFragment", "Error body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e("EventsFragment", errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("EventsFragment", "Network error: " + t.getMessage(), t);

                // Very specific guidance for the user
                String message = "Network error. Make sure your server is running at " + com.smartcampus.manouba.utils.Constants.BASE_URL;
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterEvents(String selection) {
        if (eventsAdapter == null) return;

        List<Event> filteredList = new ArrayList<>();
        if (selection.equalsIgnoreCase("All")) {
            filteredList.addAll(allEvents);
        } else if (selection.equalsIgnoreCase("Manouba Campus")) {
            for (Event event : allEvents) {
                if (isCampusLocation(event)) filteredList.add(event);
            }
        } else if (selection.equalsIgnoreCase("Other")) {
            for (Event event : allEvents) {
                if (isOtherLocation(event)) filteredList.add(event);
            }
        }
        eventsAdapter.setEvents(filteredList);
    }

    private boolean isCampusLocation(Event event) {
        String location = event.getLocation();
        if (location == null) return false;
        String lower = location.toLowerCase();
        return lower.contains("manouba") || lower.contains("campus");
    }

    private boolean isOtherLocation(Event event) {
        String location = event.getLocation();
        if (location == null || location.trim().isEmpty()) return true;
        String lower = location.toLowerCase();
        return !lower.contains("manouba") && !lower.contains("campus");
    }
}
