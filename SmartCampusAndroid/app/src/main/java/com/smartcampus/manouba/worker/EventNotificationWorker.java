package com.smartcampus.manouba.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.Constants;
import com.smartcampus.manouba.utils.SharedPrefManager;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

public class EventNotificationWorker extends Worker {

    public EventNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(getApplicationContext());
        String token = prefs.getToken();
        // Use token auth if logged in, otherwise skip
        try {
            Response<List<Event>> response = RetrofitClient.getInstance(token)
                    .getApi().getEvents().execute();

            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                List<Event> events = response.body();
                Event latestEvent = events.get(0);

                int lastEventId = prefs.getLastEventId();

                if (latestEvent.getId() > lastEventId) {
                    prefs.setLastEventId(latestEvent.getId());
                    sendNotification(latestEvent);
                }
            }
            return Result.success();
        } catch (IOException e) {
            return Result.retry();
        }
    }

    private void sendNotification(Event event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager nm = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_EVENTS,
                    "Events",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("New campus event notifications");
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), Constants.CHANNEL_EVENTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("📅 New Event: " + event.getTitle())
                .setContentText(event.getSubtitle() != null ? event.getSubtitle() : event.getDateDisplay())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(event.getDescription() != null ? event.getDescription() : ""))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(1, builder.build());
    }
}
