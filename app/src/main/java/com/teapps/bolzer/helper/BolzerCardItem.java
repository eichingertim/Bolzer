package com.teapps.bolzer.helper;

//TODO Add ID of Bolzer

import androidx.annotation.Nullable;

public class BolzerCardItem {

    String mapURL;
    String title;
    String address;
    String ID;
    String creatorNameEmailID;
    String date;
    String time;
    String members;
    String ageGroup;
    String location;

    public BolzerCardItem(String mapURL, String title, String address, String ID) {
        this.mapURL = mapURL;
        this.title = title;
        this.address = address;
        this.ID = ID;
    }

    public BolzerCardItem(String mapURL, String title, String address, String ID
            , String creatorNameEmailID, String date, String time
            , String members, String ageGroup, String location) {
        this.mapURL = mapURL;
        this.title = title;
        this.address = address;
        this.ID = ID;
        this.creatorNameEmailID = creatorNameEmailID;
        this.date = date;
        this.time = time;
        this.members = members;
        this.ageGroup = ageGroup;
        this.location = location;
    }

    public String getCreatorNameEmailID() {
        return creatorNameEmailID;
    }

    public void setCreatorNameEmailID(String creatorNameEmailID) {
        this.creatorNameEmailID = creatorNameEmailID;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMembers() {
        return members;
    }

    public void setMembers(String members) {
        this.members = members;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMapURL() {
        return mapURL;
    }

    public void setMapURL(String mapURL) {
        this.mapURL = mapURL;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        BolzerCardItem bolzerCardItem = (BolzerCardItem) obj;

        return bolzerCardItem.getID().equals(this.getID());

    }
}
