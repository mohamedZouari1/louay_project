package com.smartcampus.manouba.network;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // Auth
    @POST("auth/register/")
    Call<JsonObject> register(@Body JsonObject body);

    @POST("auth/login/")
    Call<JsonObject> login(@Body JsonObject body);

    @POST("auth/logout/")
    Call<JsonObject> logout();

    // Profile
    @GET("profile/")
    Call<JsonObject> getProfile();

    @PUT("profile/")
    Call<JsonObject> updateProfile(@Body JsonObject body);

    // Locations
    @GET("locations/")
    Call<List<JsonObject>> getLocations();

    @GET("locations/")
    Call<List<JsonObject>> getLocationsByCategory(@Query("category") String category);

    // Events
    @GET("events/")
    Call<List<JsonObject>> getEvents();

    // Stats
    @GET("stats/")
    Call<List<JsonObject>> getStats();

    // Reports
    @POST("reports/")
    Call<JsonObject> submitReport(@Body JsonObject body);

    @GET("reports/")
    Call<List<JsonObject>> getReports();

    // Favorites
    @GET("favorites/")
    Call<List<JsonObject>> getFavorites();

    @POST("favorites/")
    Call<JsonObject> addFavorite(@Body JsonObject body);

    @DELETE("favorites/{id}/")
    Call<Void> deleteFavorite(@Path("id") int id);
}
