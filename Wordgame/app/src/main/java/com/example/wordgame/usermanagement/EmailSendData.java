package com.example.wordgame.usermanagement;

public class EmailSendData {

    private String userId;
    private String date;

    public EmailSendData() { }

    public String getUserId() {
        return userId;
    }

    public String getDate() {
        return date;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
