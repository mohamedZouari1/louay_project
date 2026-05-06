package com.smartcampus.manouba.model;

public class ChatMessage {
    private String id;
    private String content;
    private String time;
    private boolean isSentByMe;
    private String imageUrl;
    private String fileUrl;

    public ChatMessage(String id, String content, String time, boolean isSentByMe, String imageUrl, String fileUrl) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = imageUrl;
        this.fileUrl = fileUrl;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public String getTime() { return time; }
    public boolean isSentByMe() { return isSentByMe; }
    public String getImageUrl() { return imageUrl; }
    public String getFileUrl() { return fileUrl; }
}
