package com.smartcampus.manouba.model;

public class ChatMessage {
    private String id;
    private String content;
    private String time;
    private boolean isSentByMe;
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private String fileType;

    /** Full constructor */
    public ChatMessage(String id, String content, String time, boolean isSentByMe,
                       String imageUrl, String fileUrl, String fileName, String fileType) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = imageUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    /** Convenience constructor used by ChatMessagesFragment */
    public ChatMessage(String content, boolean isSentByMe, String imageUrl, String time) {
        this.id = null;
        this.content = content;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = imageUrl;
        this.fileUrl = null;
        this.fileName = null;
        this.fileType = null;
    }

    public String getId()        { return id; }
    public String getContent()   { return content; }
    public String getTime()      { return time; }
    public boolean isSentByMe()  { return isSentByMe; }
    public String getImageUrl()  { return imageUrl; }
    public String getFileUrl()   { return fileUrl; }
    public String getFileName()  { return fileName; }
    public String getFileType()  { return fileType; }

    public void setTime(String time) { this.time = time; }
}
