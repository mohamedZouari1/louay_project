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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private List<Event> events;

    public EventsAdapter(List<Event> events) {
        this.events = events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        if (event == null) return;

        holder.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "No Title");
        holder.tvEventSubtitle.setText(event.getSubtitle() != null ? event.getSubtitle() : "");
        holder.tvEventDescription.setText(event.getDescription() != null ? event.getDescription() : "");
        
        // Use a formatted date version if possible, or just the raw display
        String rawDate = event.getDateDisplay();
        holder.tvEventDate.setText(rawDate != null ? rawDate : "");

        updateEventStatus(holder, rawDate);
        
        // Dynamic Image Selection
        int resId = findResourceForEvent(holder.itemView.getContext(), event);
        
        if (resId != 0) {
            Glide.with(holder.itemView.getContext())
                    .load(resId)
                    .centerCrop()
                    .placeholder(R.color.gray_light)
                    .into(holder.ivEventImage);
        } else if (event.getImageName() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getImageName())
                    .centerCrop()
                    .placeholder(R.color.gray_light)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivEventImage);
        }

        // Animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(30f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(position * 100L)
                .start();

        holder.btnViewEvent.setOnClickListener(v -> Toast.makeText(v.getContext(), "Viewing details for " + event.getTitle(), Toast.LENGTH_SHORT).show());
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
            else if (title.contains("symposium") || title.contains("sumposium")) resId = R.drawable.symposium;
            else if (title.contains("hackathon") || title.contains("hckathon") || title.contains("hackaton")) resId = R.drawable.hackaton_uma;
            else if (title.contains("networking")) resId = R.drawable.networkingday;
            else if (title.contains("green")) resId = R.drawable.greenspaces;
        }

        return resId;
    }

    private void updateEventStatus(ViewHolder holder, String dateDisplay) {
        Date eventDate = parseEndDate(dateDisplay);
        
        // Today at exactly midnight for accurate comparison
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();

        holder.tvEventStatus.setVisibility(View.VISIBLE);

        if (eventDate != null && eventDate.before(today)) {
            // PAST -> FINISHED (RED)
            holder.tvEventStatus.setText(R.string.status_finished);
            holder.tvEventStatus.setBackgroundResource(R.drawable.bg_status_finished);
            
            // Set a scaled version of the icon (14dp equivalent)
            android.graphics.drawable.Drawable icon = holder.itemView.getContext().getDrawable(R.drawable.ic_finished);
            if (icon != null) {
                int size = (int) (14 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                icon.setBounds(0, 0, size, size);
                holder.tvEventStatus.setCompoundDrawables(icon, null, null, null);
            }
        } else {
            // TODAY OR FUTURE -> UPCOMING (GREEN)
            holder.tvEventStatus.setText(R.string.status_upcoming);
            holder.tvEventStatus.setBackgroundResource(R.drawable.bg_status_upcoming);
            
            android.graphics.drawable.Drawable icon = holder.itemView.getContext().getDrawable(R.drawable.ic_upcoming);
            if (icon != null) {
                int size = (int) (14 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                icon.setBounds(0, 0, size, size);
                holder.tvEventStatus.setCompoundDrawables(icon, null, null, null);
            }
        }
    }

    private Date parseEndDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            String[] parts = dateString.split("-|to|–|—");
            String lastPart = parts[parts.length - 1].trim();
            String firstPart = parts[0].trim();

            int year = Calendar.getInstance().get(Calendar.YEAR);
            Matcher yearMatcher = Pattern.compile("(\\d{4})").matcher(dateString);
            if (yearMatcher.find() && yearMatcher.groupCount() >= 1) {
                String yearStr = yearMatcher.group(1);
                if (yearStr != null) year = Integer.parseInt(yearStr);
            }

            String monthRegex = "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Déc)";
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
            Log.e("EventsAdapter", "Parse error: " + dateString);
        }
        return null;
    }

    private int getMonthIndex(String monthStr) {
        monthStr = monthStr.toLowerCase();
        if (monthStr.startsWith("jan")) return Calendar.JANUARY;
        if (monthStr.startsWith("f")) return Calendar.FEBRUARY;
        if (monthStr.startsWith("mar")) return Calendar.MARCH;
        if (monthStr.startsWith("av") || monthStr.startsWith("ap")) return Calendar.APRIL;
        if (monthStr.startsWith("mai") || monthStr.startsWith("may")) return Calendar.MAY;
        if (monthStr.startsWith("juin") || monthStr.startsWith("jun")) return Calendar.JUNE;
        if (monthStr.startsWith("juil") || monthStr.startsWith("jul")) return Calendar.JULY;
        if (monthStr.startsWith("ao") || monthStr.startsWith("au")) return Calendar.AUGUST;
        if (monthStr.startsWith("s")) return Calendar.SEPTEMBER;
        if (monthStr.startsWith("o")) return Calendar.OCTOBER;
        if (monthStr.startsWith("n")) return Calendar.NOVEMBER;
        if (monthStr.startsWith("d")) return Calendar.DECEMBER;
        return Calendar.JANUARY;
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventTitle, tvEventSubtitle, tvEventDescription, tvEventDate, tvEventStatus;
        MaterialButton btnViewEvent;

        ViewHolder(View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.iv_event_image);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventSubtitle = itemView.findViewById(R.id.tv_event_subtitle);
            tvEventDescription = itemView.findViewById(R.id.tv_event_description);
            tvEventDate = itemView.findViewById(R.id.tv_event_date);
            btnViewEvent = itemView.findViewById(R.id.btn_view_event);
            tvEventStatus = itemView.findViewById(R.id.tv_event_status);
        }
    }
}
