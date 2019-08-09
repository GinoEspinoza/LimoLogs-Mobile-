package com.dotcompliance.limologs.ELD;

import java.util.ArrayList;

/**
 * Created by kmw on 2017.09.12.
 */

public class AvlEvent {
    // first-level properties
    public int eventCode = 0;
    public String eventType = "";
    public String eventTime = "";
    public String messageUuid = "";
    public int deviceMessageSequenceNumber = 0;
    public ArrayList<AvlItemListModel> itemList = new ArrayList<>();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int status = 0;
    // Ignition status
    public  float engineHours = 0;
    public String ignitionStatus ;
    public boolean igintionStatusGPX = false;


    // address
    public String city = "";
    public String state = "";
    public float longitude = 0;
    public float latitude = 0;


    // deviceDataConverted
    public float gpsOdometer = 0;
    public float gpsSpeed = 0;
    public float vbOdometer = 0;
    public float vbSpeed = 0;

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String serverTime = "";


    public int getEventCode() {
        return eventCode;
    }

    public void setEventCode(int eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(String messageUuid) {
        this.messageUuid = messageUuid;
    }

    public int getDeviceMessageSequenceNumber() {
        return deviceMessageSequenceNumber;
    }

    public void setDeviceMessageSequenceNumber(int deviceMessageSequenceNumber) {
        this.deviceMessageSequenceNumber = deviceMessageSequenceNumber;
    }

    public String getIgnitionStatus() {
        return ignitionStatus;
    }

    public void setIgnitionStatus(String ignitionStatus) {
        this.ignitionStatus = ignitionStatus;
    }

    public boolean isIgintionStatusGPX() {
        return igintionStatusGPX;
    }

    public void setIgintionStatusGPX(boolean igintionStatusGPX) {
        this.igintionStatusGPX = igintionStatusGPX;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getGpsOdometer() {
        return gpsOdometer;
    }

    public void setGpsOdometer(float gpsOdometer) {
        this.gpsOdometer = gpsOdometer;
    }

    public float getGpsSpeed() {
        return gpsSpeed;
    }

    public void setGpsSpeed(float gpsSpeed) {
        this.gpsSpeed = gpsSpeed;
    }

    public float getVbOdometer() {
        return vbOdometer;
    }

    public void setVbOdometer(float vbOdometer) {
        this.vbOdometer = vbOdometer;
    }

    public float getVbSpeed() {
        return vbSpeed;
    }

    public void setVbSpeed(float vbSpeed) {
        this.vbSpeed = vbSpeed;
    }

    public float getEngineHours() {
        return engineHours;
    }

    public void setEngineHours(float engineHours) {
        this.engineHours = engineHours;
    }

    public ArrayList<AvlItemListModel> getItemList() {
        return itemList;
    }

    public void setItemList(ArrayList<AvlItemListModel> itemList) {
        this.itemList = itemList;
    }
}
