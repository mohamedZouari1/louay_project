package com.smartcampus.manouba.network;

import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("auth/register/")
    Call<JsonObject> register(@Body JsonObject body);
    @POST("auth/login/")
    Call<JsonObject> login(@Body JsonObject body);
    @POST("auth/logout/")
    Call<JsonObject> logout();

    // ── Profile ─────────────────────────────────────────────────────────────
    @GET("profile/")
    Call<JsonObject> getProfile();
    @PUT("profile/")
    Call<JsonObject> updateProfile(@Body JsonObject body);

    // ── Campus data ─────────────────────────────────────────────────────────
    @GET("locations/")
    Call<List<JsonObject>> getLocations();
    @GET("locations/")
    Call<List<JsonObject>> getLocationsByCategory(@Query("category") String category);
    @GET("events/")
    Call<List<JsonObject>> getEvents();
    @GET("stats/")
    Call<List<JsonObject>> getStats();

    // ── Reports & Favorites ─────────────────────────────────────────────────
    @POST("reports/")
    Call<JsonObject> submitReport(@Body JsonObject body);
    @GET("reports/")
    Call<List<JsonObject>> getReports();
    @GET("favorites/")
    Call<List<JsonObject>> getFavorites();
    @POST("favorites/")
    Call<JsonObject> addFavorite(@Body JsonObject body);
    @DELETE("favorites/{id}/")
    Call<Void> deleteFavorite(@Path("id") int id);

    // ── Social Hub ──────────────────────────────────────────────────────────
    @GET("social/feed/")
    Call<List<JsonObject>> getFeed();
    @Multipart
    @POST("social/posts/")
    Call<JsonObject> createPost(
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image
    );

    /** Create a text-only post (no image) via JSON */
    @POST("social/posts/text/")
    Call<JsonObject> createTextPost(@Body JsonObject body);

    @POST("social/posts/{id}/like/")
    Call<JsonObject> likePost(@Path("id") int id);
    @DELETE("social/posts/{id}/like/")
    Call<JsonObject> unlikePost(@Path("id") int id);
    @GET("social/users/search/")
    Call<List<JsonObject>> searchUsers(@Query("q") String query);
    @GET("social/users/{id}/")
    Call<JsonObject> getUserProfile(@Path("id") int id);
}
