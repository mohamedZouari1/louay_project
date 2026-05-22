package com.smartcampus.manouba.utils;

public class Constants {
    /**
     * For Android Emulator, use "http://10.0.2.2:8000/api/"
     * For Real Device, use your PC's WiFi IP (e.g., "http://192.168.1.5:8000/api/")
     * Ensure the phone and PC are on the same WiFi network.
     */
    public static final String BASE_URL = "https://smart-campus-api-8e7s.onrender.com/api/";

    // Shared Preferences keys
    public static final String PREF_NAME = "SmartCampusPrefs";
    public static final String KEY_TOKEN = "auth_token";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_UNIVERSITY = "user_university";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_LAST_EVENT_ID = "last_event_id";
    public static final String KEY_LAST_UNREAD_MSG_COUNT = "last_unread_msg_count";
    public static final String KEY_LAST_NOTIFICATION_COUNT = "last_notification_count";

    // Notification channels
    public static final String CHANNEL_MESSAGES = "channel_messages";
    public static final String CHANNEL_EVENTS   = "channel_events";
    public static final String CHANNEL_SOCIAL   = "channel_social";

    // Polling intervals (milliseconds)
    public static final long POLL_CHAT_MS  = 5_000L;   // 5 seconds
    public static final long POLL_FEED_MS  = 30_000L;  // 30 seconds

    // Request codes
    public static final int REQUEST_RECORD_AUDIO = 1001;
    public static final int REQUEST_READ_MEDIA   = 1002;
}
