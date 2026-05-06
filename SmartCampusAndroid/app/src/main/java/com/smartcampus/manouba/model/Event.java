package com.smartcampus.manouba.model;

import com.google.gson.annotations.SerializedName;

public class Event {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("subtitle")
    private String subtitle;

    @SerializedName("description")
    private String description;

    @SerializedName("date_display")
    private String dateDisplay;

    @SerializedName("image_name")
    private String imageName;

    @SerializedName("location")
    private String location;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_interested")
    private boolean isInterested;

    @SerializedName("is_attending")
    private boolean isAttending;

    @SerializedName("participants_count")
    private int participantsCount;

    public Event() {
    }

    public Event(String title, String subtitle, String description, String dateDisplay, String imageName, String imageUrl) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.dateDisplay = dateDisplay;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDescription() { return description; }
    public String getDateDisplay() { return dateDisplay; }
    public String getImageName() { return imageName; }
    public String getImageUrl() { return imageUrl; }
    public String getLocation() { return location; }
    
    public boolean isInterested() { return isInterested; }
    public void setInterested(boolean interested) { isInterested = interested; }
    public boolean isAttending() { return isAttending; }
    public void setAttending(boolean attending) { isAttending = attending; }
    public int getParticipantsCount() { return participantsCount; }
    public void setParticipantsCount(int count) { participantsCount = count; }
}
