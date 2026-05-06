package com.smartcampus.manouba.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.Event;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.SharedPrefManager;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

public class EventNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "event_channel";

    public EventNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Response<List<Event>> response = RetrofitClient.getInstance().getApi().getEvents().execute();
            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                List<Event> events = response.body();
                Event latestEvent = events.get(0); // Assuming the API returns events sorted by date descending
                
                SharedPrefManager prefManager = SharedPrefManager.getInstance(getApplicationContext());
                int lastEventId = prefManager.getLastEventId();

                if (latestEvent.getId() > lastEventId) {
                    prefManager.setLastEventId(latestEvent.getId());
                    sendNotification(latestEvent);
                }
            }
            return Result.success();
        } catch (IOException e) {
            return Result.failure();
        }
    }

    private void sendNotification(Event event) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "New Events", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Event: " + event.getTitle())
                .setContentText(event.getSubtitle())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}
