package com.smartcampus.manouba.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.adapters.EventsAdapter;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.network.RetrofitClient;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventsAdapter eventsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutEmptyState;
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
        
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_events);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        swipeRefreshLayout.setOnRefreshListener(this::fetchEvents);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primary));

        fetchEvents();
    }

    private void fetchEvents() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        RetrofitClient.getInstance().getApi().getEvents().enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                Log.d("EventsFragment", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    allEvents = response.body();
                    sortEventsByDate(allEvents);
                    
                    if (allEvents.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        eventsAdapter = new EventsAdapter(new ArrayList<>(allEvents));
                        recyclerView.setAdapter(eventsAdapter);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Log.e("EventsFragment", "Network error: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error - Check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sortEventsByDate(List<Event> events) {
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event a, Event b) {
                Date aDate = parseEndDate(a != null ? a.getDateDisplay() : null);
                Date bDate = parseEndDate(b != null ? b.getDateDisplay() : null);

                if (aDate == null && bDate == null) return 0;
                if (aDate == null) return 1;
                if (bDate == null) return -1;
                return bDate.compareTo(aDate);
            }
        });
    }

    private Date parseEndDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;

        // Fast path for common formats like "Oct 19, 2025" or "Apr 30, 2025"
        String trimmed = dateString.trim();
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
        try {
            return fmt.parse(trimmed);
        } catch (ParseException ignored) {
        }

        try {
            String[] parts = dateString.split("-|to");
            String lastPart = parts[parts.length - 1].trim();
            String firstPart = parts[0].trim();

            int year = Calendar.getInstance().get(Calendar.YEAR);
            Matcher yearMatcher = Pattern.compile("(\\d{4})").matcher(dateString);
            if (yearMatcher.find() && yearMatcher.groupCount() >= 1) {
                String yearStr = yearMatcher.group(1);
                if (yearStr != null) year = Integer.parseInt(yearStr);
            }

            String monthRegex = "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";
            String monthStr = null;
            int day = -1;

            Pattern p1 = Pattern.compile(monthRegex + "[a-z]*\\s*(\\d{1,2})");
            Pattern p2 = Pattern.compile("(\\d{1,2})\\s*" + monthRegex + "[a-z]*");

            Matcher m1 = p1.matcher(lastPart);
            if (m1.find() && m1.groupCount() >= 2) {
                monthStr = m1.group(1);
                String dayStr = m1.group(2);
                if (dayStr != null) day = Integer.parseInt(dayStr);
            } else {
                Matcher m2 = p2.matcher(lastPart);
                if (m2.find() && m2.groupCount() >= 2) {
                    String dayStr = m2.group(1);
                    if (dayStr != null) day = Integer.parseInt(dayStr);
                    monthStr = m2.group(2);
                } else {
                    Matcher mDay = Pattern.compile("(\\d{1,2})").matcher(lastPart);
                    if (mDay.find() && mDay.groupCount() >= 1) {
                        String dayStr = mDay.group(1);
                        if (dayStr != null) day = Integer.parseInt(dayStr);
                    }
                    Matcher mMonth = Pattern.compile(monthRegex).matcher(firstPart);
                    if (mMonth.find() && mMonth.groupCount() >= 1) monthStr = mMonth.group(1);
                }
            }

            if (monthStr != null && day != -1) {
                Calendar res = Calendar.getInstance();
                res.set(year, getMonthIndex(monthStr), day, 0, 0, 0);
                res.set(Calendar.MILLISECOND, 0);
                return res.getTime();
            }
        } catch (Exception e) {
            Log.e("EventsFragment", "Parse error: " + dateString);
        }
        return null;
    }

    private int getMonthIndex(String monthStr) {
        String lower = monthStr.toLowerCase();
        if (lower.startsWith("jan")) return Calendar.JANUARY;
        if (lower.startsWith("f")) return Calendar.FEBRUARY;
        if (lower.startsWith("mar")) return Calendar.MARCH;
        if (lower.startsWith("ap")) return Calendar.APRIL;
        if (lower.startsWith("may")) return Calendar.MAY;
        if (lower.startsWith("jun")) return Calendar.JUNE;
        if (lower.startsWith("jul")) return Calendar.JULY;
        if (lower.startsWith("au")) return Calendar.AUGUST;
        if (lower.startsWith("s")) return Calendar.SEPTEMBER;
        if (lower.startsWith("o")) return Calendar.OCTOBER;
        if (lower.startsWith("n")) return Calendar.NOVEMBER;
        if (lower.startsWith("d")) return Calendar.DECEMBER;
        return Calendar.JANUARY;
    }
}
