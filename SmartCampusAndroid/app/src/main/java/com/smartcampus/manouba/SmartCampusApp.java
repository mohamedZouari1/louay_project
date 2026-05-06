package com.smartcampus.manouba;

import android.app.Application;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.smartcampus.manouba.worker.EventNotificationWorker;
import java.util.concurrent.TimeUnit;

public class SmartCampusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        scheduleEventNotifications();
    }

    private void scheduleEventNotifications() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(EventNotificationWorker.class, 24, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);
    }
}
