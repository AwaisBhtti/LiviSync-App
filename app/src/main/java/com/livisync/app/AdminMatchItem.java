package com.livisync.app;

public class AdminMatchItem {
    private String matchId;
    private String user1Uid, user1Name, user1Email;
    private String user2Uid, user2Name, user2Email;
    private long timestamp;

    public AdminMatchItem(String matchId, String user1Uid, String user1Name, String user1Email,
                          String user2Uid, String user2Name, String user2Email, long timestamp) {
        this.matchId = matchId;
        this.user1Uid = user1Uid;
        this.user1Name = user1Name;
        this.user1Email = user1Email;
        this.user2Uid = user2Uid;
        this.user2Name = user2Name;
        this.user2Email = user2Email;
        this.timestamp = timestamp;
    }

    public String getMatchId() { return matchId; }
    public String getUser1Name() { return user1Name; }
    public String getUser1Email() { return user1Email; }
    public String getUser2Name() { return user2Name; }
    public String getUser2Email() { return user2Email; }
    public long getTimestamp() { return timestamp; }
}