package com.livisync.app;

public class ChatItem {
    private String matchId;
    private String otherUid;
    private String otherName;
    private String lastMessage;
    private boolean unread;

    public ChatItem(String matchId, String otherUid, String otherName, String lastMessage, boolean unread) {
        this.matchId = matchId;
        this.otherUid = otherUid;
        this.otherName = otherName;
        this.lastMessage = lastMessage;
        this.unread = unread;
    }

    public String getMatchId() { return matchId; }
    public String getOtherUid() { return otherUid; }
    public String getOtherName() { return otherName; }
    public String getLastMessage() { return lastMessage; }
    public boolean isUnread() { return unread; }
}