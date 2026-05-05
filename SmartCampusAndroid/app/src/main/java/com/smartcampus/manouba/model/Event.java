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

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public String getDateDisplay() {
        return dateDisplay;
    }

    public String getImageName() {
        return imageName;
    }

    public String getLocation() {
        return location;
    }
}

