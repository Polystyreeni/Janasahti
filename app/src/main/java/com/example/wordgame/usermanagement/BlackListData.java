package com.example.wordgame.usermanagement;

/**
 * Ban list data. Used for Firebase.
 */
public class BlackListData {
    private String userID;
    private String restrictionReason;
    private String restrictionDate;

    public BlackListData() { }

    public String getRestrictionDate() {
        return restrictionDate;
    }

    public void setRestrictionDate(String restrictionDate) {
        this.restrictionDate = restrictionDate;
    }

    public String getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(String restrictionReason) {
        this.restrictionReason = restrictionReason;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
