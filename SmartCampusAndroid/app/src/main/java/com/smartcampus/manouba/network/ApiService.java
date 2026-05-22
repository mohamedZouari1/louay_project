package com.smartcampus.manouba.network;

import com.google.gson.JsonObject;
import com.smartcampus.manouba.model.Event;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

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
    @GET("social/suggestions/")
    Call<List<JsonObject>> getSuggestions();

    // ── Campus data ─────────────────────────────────────────────────────────
    @GET("locations/")
    Call<List<JsonObject>> getLocations();
    @GET("locations/")
    Call<List<JsonObject>> getLocationsByCategory(@Query("category") String category);
    @GET("events/")
    Call<List<Event>> getEvents();
    @POST("events/{id}/register/")
    Call<JsonObject> registerEvent(@Path("id") int id, @Body JsonObject body);
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

    /** Create post with image (multipart) */
    @Multipart
    @POST("social/posts/")
    Call<JsonObject> createPost(
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image
    );

    /** Create post with file/audio (multipart) */
    @Multipart
    @POST("social/posts/")
    Call<JsonObject> createPostWithFile(
            @Part("content") RequestBody content,
            @Part MultipartBody.Part file
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

    @POST("social/users/{id}/follow/")
    Call<JsonObject> followUser(@Path("id") int id);
    @DELETE("social/users/{id}/follow/")
    Call<JsonObject> unfollowUser(@Path("id") int id);
    @GET("social/users/me/following/")
    Call<List<JsonObject>> getFollowing();

    @GET("social/posts/{id}/comments/")
    Call<List<JsonObject>> getPostComments(@Path("id") int id);
    @POST("social/posts/{id}/comments/")
    Call<JsonObject> addComment(@Path("id") int id, @Body JsonObject body);
    @POST("social/posts/{id}/repost/")
    Call<JsonObject> repost(@Path("id") int id, @Body JsonObject body);

    // ── Chat ──────────────────────────────────────────────────────────────
    @GET("chat/conversations/")
    Call<List<JsonObject>> getConversations();
    @GET("chat/messages/{id}/")
    Call<List<JsonObject>> getMessages(@Path("id") int otherUserId);
    @POST("chat/messages/{id}/")
    Call<JsonObject> sendMessage(@Path("id") int otherUserId, @Body JsonObject body);

    /** Send image-only message */
    @Multipart
    @POST("chat/messages/{id}/")
    Call<JsonObject> sendImageMessage(
            @Path("id") int otherUserId,
            @Part("content") RequestBody content,
            @Part("reply_to") RequestBody replyTo,
            @Part MultipartBody.Part image
    );

    /** Send file/audio-only message */
    @Multipart
    @POST("chat/messages/{id}/")
    Call<JsonObject> sendFileMessage(
            @Path("id") int otherUserId,
            @Part("content") RequestBody content,
            @Part("reply_to") RequestBody replyTo,
            @Part MultipartBody.Part file
    );

    @GET("chat/unread/")
    Call<JsonObject> getUnreadCount();

    // ── Notifications ──────────────────────────────────────────────────────
    @GET("notifications/")
    Call<List<JsonObject>> getNotifications();
    @POST("notifications/read/")
    Call<JsonObject> markNotificationsRead();
    @GET("notifications/count/")
    Call<JsonObject> getNotificationCount();

    // ── Profile Image ──────────────────────────────────────────────────────
    @Multipart
    @POST("profile/image/")
    Call<JsonObject> updateProfileImage(@Part MultipartBody.Part avatar);
}
