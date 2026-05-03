package com.livisync.app;

public class UserItem {
    private String uid;
    private String name;
    private String email;
    private String role;

    public UserItem(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}