package com.smartcampus.manouba.utils;

public class Constants {
    /**
     * For Android Emulator, use "http://10.0.2.2:8000/api/"
     * For Real Device, use your PC's WiFi IP (e.g., "http://192.168.1.5:8000/api/")
     * Ensure the phone and PC are on the same WiFi network.
     */
    public static final String BASE_URL = "http://10.0.2.2:8000/api/";

    public static final String PREF_NAME = "SmartCampusPrefs";
    public static final String KEY_TOKEN = "auth_token";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_UNIVERSITY = "user_university";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
}
