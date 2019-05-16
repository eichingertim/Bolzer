package com.teapps.bolzer.helper;

//TODO Add ID of Bolzer

import androidx.annotation.Nullable;

public class BolzerCardItem {

    String mapURL;
    String title;
    String address;
    String ID;

    public BolzerCardItem(String mapURL, String title, String address, String ID) {
        this.mapURL = mapURL;
        this.title = title;
        this.address = address;
        this.ID = ID;
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
