package com.smartcampus.manouba.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static SharedPrefManager instance;
    private final SharedPreferences prefs;

    private SharedPrefManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveUser(String name, String email, String university, int id) {
        prefs.edit()
                .putString(Constants.KEY_USER_NAME, name)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_UNIVERSITY, university)
                .putInt(Constants.KEY_USER_ID, id)
                .apply();
    }

    public void saveUserName(String name) {
        prefs.edit().putString(Constants.KEY_USER_NAME, name).apply();
    }

    public void saveUserUniversity(String university) {
        prefs.edit().putString(Constants.KEY_USER_UNIVERSITY, university).apply();
    }

    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, "Student");
    }

    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, "");
    }

    public String getUserUniversity() {
        return prefs.getString(Constants.KEY_USER_UNIVERSITY, "");
    }

    public int getUserId() {
        return prefs.getInt(Constants.KEY_USER_ID, -1);
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(Constants.KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        prefs.edit().putBoolean(Constants.KEY_FIRST_LAUNCH, false).apply();
    }

    public void logout() {
        prefs.edit()
                .remove(Constants.KEY_TOKEN)
                .remove(Constants.KEY_USER_NAME)
                .remove(Constants.KEY_USER_EMAIL)
                .remove(Constants.KEY_USER_UNIVERSITY)
                .remove(Constants.KEY_USER_ID)
                .apply();
    }

    public int getLastEventId() {
        return prefs.getInt(Constants.KEY_LAST_EVENT_ID, -1);
    }

    public void setLastEventId(int id) {
        prefs.edit().putInt(Constants.KEY_LAST_EVENT_ID, id).apply();
    }

    public int getLastUnreadMsgCount() {
        return prefs.getInt(Constants.KEY_LAST_UNREAD_MSG_COUNT, 0);
    }

    public void setLastUnreadMsgCount(int count) {
        prefs.edit().putInt(Constants.KEY_LAST_UNREAD_MSG_COUNT, count).apply();
    }

    public int getLastNotificationCount() {
        return prefs.getInt(Constants.KEY_LAST_NOTIFICATION_COUNT, 0);
    }

    public void setLastNotificationCount(int count) {
        prefs.edit().putInt(Constants.KEY_LAST_NOTIFICATION_COUNT, count).apply();
    }
}
