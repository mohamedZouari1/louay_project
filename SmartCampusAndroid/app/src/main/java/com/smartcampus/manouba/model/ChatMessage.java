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
    private String replyToId;
    private String replyToSenderName;
    private String replyToContent;

    /** Full constructor */
    public ChatMessage(String id, String content, String time, boolean isSentByMe,
                       String imageUrl, String fileUrl, String fileName, String fileType,
                       String replyToId, String replyToSenderName, String replyToContent) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.isSentByMe = isSentByMe;
        this.imageUrl = imageUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.replyToId = replyToId;
        this.replyToSenderName = replyToSenderName;
        this.replyToContent = replyToContent;
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
        this.replyToId = null;
        this.replyToSenderName = null;
        this.replyToContent = null;
    }

    public String getId()        { return id; }
    public String getContent()   { return content; }
    public String getTime()      { return time; }
    public boolean isSentByMe()  { return isSentByMe; }
    public String getImageUrl()  { return imageUrl; }
    public String getFileUrl()   { return fileUrl; }
    public String getFileName()  { return fileName; }
    public String getFileType()  { return fileType; }
    public String getReplyToId() { return replyToId; }
    public String getReplyToSenderName() { return replyToSenderName; }
    public String getReplyToContent() { return replyToContent; }

    public void setTime(String time) { this.time = time; }
}
