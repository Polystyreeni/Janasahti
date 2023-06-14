package com.example.wordgame;

public class UserProfile {
    private String userId;
    private String userName;
    private String lastLogin;

    public UserProfile(String userId, String userName, String lastLogin) {
        this.userId = userId;
        this.userName = userName;
        this.lastLogin = lastLogin;
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

    public String getLastLogin() {return this.lastLogin;}
    public void setLastLogin(String lastLogin) {this.lastLogin = lastLogin;}
}
