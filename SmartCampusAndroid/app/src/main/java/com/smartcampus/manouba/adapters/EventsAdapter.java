package com.smartcampus.manouba.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.Event;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private final List<Event> events;

    public EventsAdapter(List<Event> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        
        // Auto-width: match_parent for vertical, 320dp for horizontal
        if (parent instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) parent;
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            if (lm instanceof androidx.recyclerview.widget.LinearLayoutManager) {
                androidx.recyclerview.widget.LinearLayoutManager llm = (androidx.recyclerview.widget.LinearLayoutManager) lm;
                if (llm.getOrientation() == androidx.recyclerview.widget.LinearLayoutManager.VERTICAL) {
                    ViewGroup.LayoutParams lp = view.getLayoutParams();
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        view.setLayoutParams(lp);
                    }
                }
            }
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventSubtitle.setText(event.getSubtitle());
        holder.tvEventDescription.setText(event.getDescription());
        holder.tvEventDate.setText(event.getDateDisplay());

        if (event.getParticipantsCount() > 0) {
            holder.tvParticipantsCount.setVisibility(View.VISIBLE);
            holder.tvParticipantsCount.setText(event.getParticipantsCount() + " going");
        } else {
            holder.tvParticipantsCount.setVisibility(View.GONE);
        }

        boolean isFinished = updateEventStatus(holder, event.getDateDisplay());
        
        // Hide buttons if event is finished
        View btnRow = (View) holder.btnParticipate.getParent();
        if (btnRow != null) {
            btnRow.setVisibility(isFinished ? View.GONE : View.VISIBLE);
        }

        String imageUrl = event.getImageUrl();
        int imageRes = findResourceForEvent(holder.itemView.getContext(), event);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.uma)
                    .error(imageRes != 0 ? imageRes : android.R.drawable.ic_menu_gallery)
                    .into(holder.ivEventImage);
        } else if (imageRes != 0) {
            Glide.with(holder.itemView.getContext())
                    .load(imageRes)
                    .centerCrop()
                    .into(holder.ivEventImage);
        }

        // Update button states
        if (event.isAttending()) {
            holder.btnParticipate.setText("Attending");
            holder.btnParticipate.setIconResource(R.drawable.ic_check);
        } else {
            holder.btnParticipate.setText("Participate");
            holder.btnParticipate.setIconResource(R.drawable.ic_plus);
        }

        if (event.isInterested()) {
            holder.btnInterested.setText("Saved");
            holder.btnInterested.setIconResource(R.drawable.ic_check);
        } else {
            holder.btnInterested.setText("Interested");
            holder.btnInterested.setIconResource(R.drawable.ic_plus);
        }

        holder.btnParticipate.setOnClickListener(v -> handleAction(v.getContext(), event, "participate", position));
        holder.btnInterested.setOnClickListener(v -> handleAction(v.getContext(), event, "interested", position));
    }

    private int findResourceForEvent(Context context, Event event) {
        String imageName = event.getImageName();
        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
        int resId = 0;

        if (imageName != null && !imageName.isEmpty()) {
            String cleanName = imageName.toLowerCase().trim();
            if (cleanName.contains(".")) cleanName = cleanName.substring(0, cleanName.lastIndexOf('.'));
            cleanName = cleanName.replace(" ", "_").replace("-", "_");
            resId = context.getResources().getIdentifier(cleanName, "drawable", context.getPackageName());
        }

        if (resId == 0) {
            if (title.contains("culture")) resId = R.drawable.cultureday;
            else if (title.contains("symposium")) resId = R.drawable.symposium;
            else if (title.contains("hackathon")) resId = R.drawable.hackaton_uma;
            else if (title.contains("networking")) resId = R.drawable.networkingday;
            else if (title.contains("green")) resId = R.drawable.greenspaces;
        }
        return resId;
    }

    private boolean updateEventStatus(ViewHolder holder, String dateDisplay) {
        Date eventDate = parseEndDate(dateDisplay);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();
        
        holder.tvEventStatus.setVisibility(View.VISIBLE);
        boolean isFinished = eventDate != null && eventDate.before(today);
        
        Log.d("EventsAdapter", "Event: " + dateDisplay + " | Parsed: " + (eventDate != null ? eventDate.toString() : "null") + " | Finished: " + isFinished);

        if (isFinished) {
            holder.tvEventStatus.setText("Finished");
            holder.tvEventStatus.setBackgroundResource(R.drawable.bg_status_finished);
        } else {
            holder.tvEventStatus.setText("Upcoming");
            holder.tvEventStatus.setBackgroundResource(R.drawable.bg_status_upcoming);
        }
        return isFinished;
    }

    private Date parseEndDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            // Support multiple separators
            String[] parts = dateString.split("-|to|–|—|au");
            String lastPart = parts[parts.length - 1].trim();
            String firstPart = parts[0].trim();

            int year = Calendar.getInstance().get(Calendar.YEAR);
            Matcher yearMatcher = Pattern.compile("(\\d{4})").matcher(dateString);
            if (yearMatcher.find()) {
                year = Integer.parseInt(yearMatcher.group(1));
            }

            String monthRegex = "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Janv|Fév|Mars|Avr|Mai|Juin|Juil|Août|Sept|Oct|Nov|Déc)";
            String monthStr = null;
            int day = -1;

            // Try "Month Day" format
            Pattern p1 = Pattern.compile(monthRegex + "[a-z]*\\s*(\\d{1,2})");
            // Try "Day Month" format
            Pattern p2 = Pattern.compile("(\\d{1,2})\\s*" + monthRegex + "[a-z]*");

            Matcher m1 = p1.matcher(lastPart);
            if (m1.find()) {
                monthStr = m1.group(1);
                day = Integer.parseInt(m1.group(2));
            } else {
                Matcher m2 = p2.matcher(lastPart);
                if (m2.find()) {
                    day = Integer.parseInt(m2.group(1));
                    monthStr = m2.group(2);
                } else {
                    // Fallback to first part if last part didn't have month
                    Matcher m1f = p1.matcher(firstPart);
                    if (m1f.find()) {
                        monthStr = m1f.group(1);
                        if (day == -1) day = Integer.parseInt(m1f.group(2));
                    }
                }
            }

            if (monthStr != null && day != -1) {
                Calendar res = Calendar.getInstance();
                res.set(year, getMonthIndex(monthStr), day, 23, 59, 59); // Set to end of day
                return res.getTime();
            }
        } catch (Exception e) {
            Log.e("EventsAdapter", "Error parsing date: " + dateString, e);
        }
        return null;
    }

    private int getMonthIndex(String monthStr) {
        String s = monthStr.toLowerCase();
        if (s.startsWith("jan")) return Calendar.JANUARY;
        if (s.startsWith("f")) return Calendar.FEBRUARY;
        if (s.startsWith("mar")) return Calendar.MARCH;
        if (s.startsWith("av") || s.startsWith("ap")) return Calendar.APRIL;
        if (s.startsWith("mai") || s.startsWith("may")) return Calendar.MAY;
        if (s.startsWith("juin") || s.startsWith("jun")) return Calendar.JUNE;
        if (s.startsWith("juil") || s.startsWith("jul")) return Calendar.JULY;
        if (s.startsWith("ao") || s.startsWith("au")) return Calendar.AUGUST;
        if (s.startsWith("s")) return Calendar.SEPTEMBER;
        if (s.startsWith("o")) return Calendar.OCTOBER;
        if (s.startsWith("n")) return Calendar.NOVEMBER;
        if (s.startsWith("d")) return Calendar.DECEMBER;
        return Calendar.JANUARY;
    }

    private void handleAction(Context context, Event event, String action, int position) {
        String token = com.smartcampus.manouba.utils.SharedPrefManager.getInstance(context).getToken();
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("action", action);

        com.smartcampus.manouba.network.RetrofitClient.getInstance(token).getApi()
                .registerEvent(event.getId(), body)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject res = response.body();
                            event.setInterested(res.get("is_interested").getAsBoolean());
                            event.setAttending(res.get("is_attending").getAsBoolean());
                            event.setParticipantsCount(res.get("participants_count").getAsInt());
                            notifyItemChanged(position);
                            String msg = action.equals("participate") ? 
                                (event.isAttending() ? "Registered for event!" : "Registration cancelled") :
                                (event.isInterested() ? "Added to interested" : "Removed from interested");
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventTitle, tvEventSubtitle, tvEventDescription, tvEventDate, tvEventStatus, tvParticipantsCount;
        MaterialButton btnParticipate, btnInterested;

        ViewHolder(View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.iv_event_image);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventSubtitle = itemView.findViewById(R.id.tv_event_subtitle);
            tvEventDescription = itemView.findViewById(R.id.tv_event_description);
            tvEventDate = itemView.findViewById(R.id.tv_event_date);
            tvParticipantsCount = itemView.findViewById(R.id.tv_participants_count);
            btnParticipate = itemView.findViewById(R.id.btn_participate);
            btnInterested = itemView.findViewById(R.id.btn_interested);
            tvEventStatus = itemView.findViewById(R.id.tv_event_status);
        }
    }
}
