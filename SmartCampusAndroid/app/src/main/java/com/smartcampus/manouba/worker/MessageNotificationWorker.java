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
import com.google.gson.JsonObject;
import com.smartcampus.manouba.MainActivity;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.network.RetrofitClient;
import com.smartcampus.manouba.utils.Constants;
import com.smartcampus.manouba.utils.SharedPrefManager;
import java.io.IOException;
import retrofit2.Response;

public class MessageNotificationWorker extends Worker {

    public MessageNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(getApplicationContext());
        String token = prefs.getToken();
        if (token == null) return Result.success(); // Not logged in

        try {
            Response<JsonObject> response = RetrofitClient.getInstance(token)
                    .getApi().getUnreadCount().execute();

            if (response.isSuccessful() && response.body() != null) {
                int unread = response.body().has("unread")
                        ? response.body().get("unread").getAsInt() : 0;
                int lastKnown = prefs.getLastUnreadMsgCount();

                if (unread > 0 && unread > lastKnown) {
                    prefs.setLastUnreadMsgCount(unread);
                    showNotification(unread);
                } else if (unread == 0) {
                    // Reset so next time messages arrive we notify again
                    prefs.setLastUnreadMsgCount(0);
                }
            }
            return Result.success();
        } catch (IOException e) {
            return Result.retry();
        }
    }

    private void showNotification(int unreadCount) {
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
                    Constants.CHANNEL_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("New chat message notifications");
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String text = unreadCount == 1
                ? "You have 1 new message"
                : "You have " + unreadCount + " new messages";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), Constants.CHANNEL_MESSAGES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Smart Campus")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(2, builder.build());
    }
}
