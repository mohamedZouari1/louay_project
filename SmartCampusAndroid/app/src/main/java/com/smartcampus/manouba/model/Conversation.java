package com.smartcampus.manouba.model;

public class Conversation {
    private String id;
    private String name;
    private String lastMessage;
    private String time;
    private boolean isGroup;
    private int unreadCount;
    private boolean isOnline;

    public Conversation(String id, String name, String lastMessage, String time, boolean isGroup, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.isGroup = isGroup;
        this.isOnline = isOnline;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public boolean isGroup() { return isGroup; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isOnline() { return isOnline; }
    
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
