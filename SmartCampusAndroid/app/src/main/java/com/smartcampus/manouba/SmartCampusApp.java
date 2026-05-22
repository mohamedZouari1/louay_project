package com.smartcampus.manouba;

import android.app.Application;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.smartcampus.manouba.worker.EventNotificationWorker;
import com.smartcampus.manouba.worker.MessageNotificationWorker;
import java.util.concurrent.TimeUnit;

public class SmartCampusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        scheduleEventNotifications();
        scheduleMessageNotifications();
    }

    private void scheduleEventNotifications() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                EventNotificationWorker.class, 6, TimeUnit.HOURS)
                .build();
        // Use KEEP so we never stack duplicate workers on restart
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "event_notification_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    private void scheduleMessageNotifications() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                MessageNotificationWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "message_notification_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}
