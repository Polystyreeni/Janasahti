package com.example.wordgame;

public class UserProfile {
    private String userId;
    private String userName;

    public UserProfile(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public UserProfile() {

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
